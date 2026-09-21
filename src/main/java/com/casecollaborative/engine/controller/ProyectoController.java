package com.casecollaborative.engine.controller;

import com.casecollaborative.engine.model.entity.Proyecto;
import com.casecollaborative.engine.model.entity.Usuario;
import com.casecollaborative.engine.repository.ProyectoRepository;
import com.casecollaborative.engine.repository.UsuarioRepository;
import com.casecollaborative.engine.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/proyectos")
public class ProyectoController {

    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final com.casecollaborative.engine.repository.DiagramaUmlRepository diagramaRepository;

    public ProyectoController(ProyectoRepository proyectoRepository, 
                              UsuarioRepository usuarioRepository,
                              com.casecollaborative.engine.repository.DiagramaUmlRepository diagramaRepository) {
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.diagramaRepository = diagramaRepository;
    }

    @PostMapping
    public ResponseEntity<?> crearProyecto(@RequestBody Map<String, String> payload) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).body("No autorizado");
        }
        
        CustomUserDetails userDetails = (CustomUserDetails) principal;
        Usuario usuario = usuarioRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Proyecto proyecto = new Proyecto();
        proyecto.setNombre(payload.getOrDefault("nombre", "Nuevo Proyecto"));
        proyecto.setDescripcion(payload.get("descripcion"));
        proyecto.setPropietario(usuario);
        proyecto.setEstado("ACTIVO");

        Proyecto guardado = proyectoRepository.save(proyecto);

        // Crear diagrama UML por defecto para el proyecto
        com.casecollaborative.engine.model.entity.DiagramaUml diagrama = new com.casecollaborative.engine.model.entity.DiagramaUml();
        diagrama.setProyecto(guardado);
        diagrama.setNombre(guardado.getNombre());
        diagrama.setVersion(1);
        diagrama.setZoomCanvas(1.0);
        diagrama.setPanX(0.0);
        diagrama.setPanY(0.0);
        diagramaRepository.save(diagrama);
        
        return ResponseEntity.ok(Map.of(
            "id", guardado.getId(),
            "nombre", guardado.getNombre()
        ));
    }

    @GetMapping
    public ResponseEntity<?> obtenerProyectos() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).body("No autorizado");
        }
        
        CustomUserDetails userDetails = (CustomUserDetails) principal;
        java.util.List<Proyecto> proyectos = proyectoRepository.findByPropietarioId(userDetails.getId());
        
        java.util.List<Map<String, Object>> result = proyectos.stream().map(p -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", p.getId());
            map.put("nombre", p.getNombre());
            map.put("fechaCreacion", p.getFechaCreacion() != null ? p.getFechaCreacion().toString() : "");
            return map;
        }).toList();
        
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarProyecto(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return ResponseEntity.status(401).body("No autorizado");
        }

        return proyectoRepository.findById(id).map(proyecto -> {
            if (!proyecto.getPropietario().getId().equals(userDetails.getId())) {
                return ResponseEntity.status(403).body("No tienes permisos para modificar este proyecto");
            }
            if (payload.containsKey("nombre") && payload.get("nombre") != null && !payload.get("nombre").trim().isEmpty()) {
                proyecto.setNombre(payload.get("nombre").trim());
            }
            if (payload.containsKey("descripcion")) {
                proyecto.setDescripcion(payload.get("descripcion"));
            }
            proyectoRepository.save(proyecto);
            return ResponseEntity.ok(Map.of("id", proyecto.getId(), "nombre", proyecto.getNombre()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProyecto(@PathVariable Long id) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return ResponseEntity.status(401).body("No autorizado");
        }

        return proyectoRepository.findById(id).map(proyecto -> {
            if (!proyecto.getPropietario().getId().equals(userDetails.getId())) {
                return ResponseEntity.status(403).body("No tienes permisos para eliminar este proyecto");
            }
            proyectoRepository.delete(proyecto);
            return ResponseEntity.ok(Map.of("mensaje", "Proyecto eliminado exitosamente"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
