package com.casecollaborative.engine.service.impl;

import com.casecollaborative.engine.dto.request.LoginRequest;
import com.casecollaborative.engine.dto.request.RegistroRequest;
import com.casecollaborative.engine.dto.response.AuthResponse;
import com.casecollaborative.engine.exception.DuplicateResourceException;
import com.casecollaborative.engine.model.entity.Rol;
import com.casecollaborative.engine.model.entity.Usuario;
import com.casecollaborative.engine.model.entity.UsuarioRol;
import com.casecollaborative.engine.model.entity.UsuarioRolId;
import com.casecollaborative.engine.repository.RolRepository;
import com.casecollaborative.engine.repository.UsuarioRepository;
import com.casecollaborative.engine.repository.UsuarioRolRepository;
import com.casecollaborative.engine.security.JwtService;
import com.casecollaborative.engine.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new DuplicateResourceException("El correo ya está registrado.");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre())
                .apellido(request.apellido())
                .correo(request.correo())
                .passwordHash(passwordEncoder.encode(request.password()))
                .activo(true)
                .build();
        
        usuarioRepository.save(usuario);

        Rol rolColaborador = rolRepository.findByNombre("COLABORADOR")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("COLABORADOR").build()));

        UsuarioRol usuarioRol = UsuarioRol.builder()
                .id(new UsuarioRolId(usuario.getId(), rolColaborador.getId()))
                .usuario(usuario)
                .rol(rolColaborador)
                .build();
        
        usuarioRolRepository.save(usuarioRol);

        List<String> rolesNames = List.of(rolColaborador.getNombre());
        String jwtToken = jwtService.generateToken(usuario.getId(), usuario.getCorreo(), rolesNames);

        return new AuthResponse(
                jwtToken,
                "Bearer",
                usuario.getId(),
                usuario.getNombre() + " " + usuario.getApellido(),
                usuario.getCorreo(),
                rolesNames
        );
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.correo(),
                        request.password()
                )
        );

        Usuario usuario = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow();
        
        usuario.setUltimoAcceso(ZonedDateTime.now());
        usuarioRepository.save(usuario);

        List<String> rolesNames = usuarioRolRepository.findAll().stream()
                .filter(ur -> ur.getUsuario().getId().equals(usuario.getId()))
                .map(ur -> ur.getRol().getNombre())
                .collect(Collectors.toList());

        String jwtToken = jwtService.generateToken(usuario.getId(), usuario.getCorreo(), rolesNames);

        return new AuthResponse(
                jwtToken,
                "Bearer",
                usuario.getId(),
                usuario.getNombre() + " " + usuario.getApellido(),
                usuario.getCorreo(),
                rolesNames
        );
    }
}
