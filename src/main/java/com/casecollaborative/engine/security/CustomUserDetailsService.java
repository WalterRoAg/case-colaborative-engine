package com.casecollaborative.engine.security;

import com.casecollaborative.engine.model.entity.Usuario;
import com.casecollaborative.engine.model.entity.UsuarioRol;
import com.casecollaborative.engine.repository.UsuarioRepository;
import com.casecollaborative.engine.repository.UsuarioRolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + username));

        List<UsuarioRol> roles = usuarioRolRepository.findAll().stream()
                .filter(ur -> ur.getUsuario().getId().equals(usuario.getId()))
                .toList();

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(ur -> new SimpleGrantedAuthority("ROLE_" + ur.getRol().getNombre()))
                .collect(Collectors.toList());

        return new CustomUserDetails(usuario.getId(), usuario.getCorreo(), usuario.getPasswordHash(), authorities);
    }
}
