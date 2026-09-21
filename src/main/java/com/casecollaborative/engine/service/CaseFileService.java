package com.casecollaborative.engine.service;

import com.casecollaborative.engine.dto.casefile.*;
import com.casecollaborative.engine.model.entity.*;
import com.casecollaborative.engine.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CaseFileService {

    private final DiagramaUmlRepository diagramaRepository;
    private final ClaseRepository claseRepository;
    private final AtributoRepository atributoRepository;
    private final RelacionClaseRepository relacionRepository;
    private final ProyectoRepository proyectoRepository;
    private final ObjectMapper objectMapper;

    public CaseFileService(DiagramaUmlRepository diagramaRepository,
                           ClaseRepository claseRepository,
                           AtributoRepository atributoRepository,
                           RelacionClaseRepository relacionRepository,
                           ProyectoRepository proyectoRepository,
                           ObjectMapper objectMapper) {
        this.diagramaRepository = diagramaRepository;
        this.claseRepository = claseRepository;
        this.atributoRepository = atributoRepository;
        this.relacionRepository = relacionRepository;
        this.proyectoRepository = proyectoRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public byte[] exportarProyectoACase(Long proyectoId) {
        var diagramaOpt = diagramaRepository.findByProyectoId(proyectoId);
        CaseFileDTO caseFile;

        if (diagramaOpt.isPresent()) {
            DiagramaUml diagrama = diagramaOpt.get();
            List<Clase> clasesEnt = claseRepository.findByDiagramaId(diagrama.getId());
            List<RelacionClase> relEnt = relacionRepository.findByDiagramaId(diagrama.getId());

            List<ClaseDTO> clasesDTO = clasesEnt.stream().map(c -> {
                List<AtributoDTO> atributosDTO = atributoRepository.findByClaseId(c.getId()).stream()
                        .map(a -> new AtributoDTO(a.getId(), a.getNombre(), a.getTipoDato(), a.getVisibilidad(), a.getEsPk(), a.getOrden()))
                        .collect(Collectors.toList());
                return new ClaseDTO(c.getId(), c.getNombre(), c.getVisibilidad(), c.getPosX(), c.getPosY(), atributosDTO);
            }).collect(Collectors.toList());

            List<RelacionDTO> relacionesDTO = relEnt.stream()
                    .map(r -> new RelacionDTO(r.getClaseOrigen().getId(), r.getClaseDestino().getId(), r.getTipoRelacion(), r.getCardinalidadOrigen(), r.getCardinalidadDestino()))
                    .collect(Collectors.toList());

            caseFile = new CaseFileDTO(
                    diagrama.getNombre(), diagrama.getVersion(), diagrama.getZoomCanvas(), diagrama.getPanX(), diagrama.getPanY(),
                    clasesDTO, relacionesDTO
            );
        } else {
            caseFile = new CaseFileDTO("Nuevo Diagrama", 1, 1.0, 0.0, 0.0, List.of(), List.of());
        }

        try {
            return objectMapper.writeValueAsBytes(caseFile);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al serializar el archivo .case", e);
        }
    }

    @Transactional
    public void importarProyectoDesdeCase(Long proyectoId, String jsonContent) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        try {
            CaseFileDTO caseFile = objectMapper.readValue(jsonContent, CaseFileDTO.class);
            
            // Limpiar diagrama anterior
            diagramaRepository.findByProyectoId(proyectoId).ifPresent(d -> {
                relacionRepository.deleteAll(relacionRepository.findByDiagramaId(d.getId()));
                List<Clase> clases = claseRepository.findByDiagramaId(d.getId());
                for (Clase c : clases) {
                    atributoRepository.deleteAll(atributoRepository.findByClaseId(c.getId()));
                }
                claseRepository.deleteAll(clases);
                diagramaRepository.delete(d);
            });

            DiagramaUml diagrama = new DiagramaUml();
            diagrama.setProyecto(proyecto);
            diagrama.setNombre(caseFile.nombreDiagrama());
            diagrama.setVersion(caseFile.version() + 1);
            diagrama.setZoomCanvas(caseFile.zoomCanvas());
            diagrama.setPanX(caseFile.panX());
            diagrama.setPanY(caseFile.panY());
            diagrama = diagramaRepository.save(diagrama);

            Map<Long, Clase> oldIdToNewClassMap = new HashMap<>();

            for (ClaseDTO cDto : caseFile.clases()) {
                Clase clase = new Clase();
                clase.setDiagrama(diagrama);
                clase.setNombre(cDto.nombre());
                clase.setVisibilidad(cDto.visibilidad());
                clase.setPosX(cDto.posX());
                clase.setPosY(cDto.posY());
                clase = claseRepository.save(clase);
                oldIdToNewClassMap.put(cDto.idOriginal(), clase);

                for (AtributoDTO aDto : cDto.atributos()) {
                    Atributo attr = new Atributo();
                    attr.setClase(clase);
                    attr.setNombre(aDto.nombre());
                    attr.setTipoDato(aDto.tipoDato());
                    attr.setVisibilidad(aDto.visibilidad());
                    attr.setEsPk(aDto.esPk());
                    attr.setOrden(aDto.orden());
                    atributoRepository.save(attr);
                }
            }

            for (RelacionDTO rDto : caseFile.relaciones()) {
                RelacionClase rel = new RelacionClase();
                rel.setDiagrama(diagrama);
                rel.setClaseOrigen(oldIdToNewClassMap.get(rDto.origenIdOriginal()));
                rel.setClaseDestino(oldIdToNewClassMap.get(rDto.destinoIdOriginal()));
                rel.setTipoRelacion(rDto.tipoRelacion());
                rel.setCardinalidadOrigen(rDto.cardinalidadOrigen());
                rel.setCardinalidadDestino(rDto.cardinalidadDestino());
                relacionRepository.save(rel);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al deserializar el archivo .case", e);
        }
    }
}
