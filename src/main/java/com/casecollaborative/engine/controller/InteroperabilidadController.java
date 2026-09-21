package com.casecollaborative.engine.controller;

import com.casecollaborative.engine.model.entity.DiagramaUml;
import com.casecollaborative.engine.repository.DiagramaUmlRepository;
import com.casecollaborative.engine.service.CaseFileService;
import com.casecollaborative.engine.service.ImageDiagramImportService;
import com.casecollaborative.engine.service.XmiInteroperabilityService;
import com.casecollaborative.engine.service.generator.BackendGeneratorRegistry;
import com.casecollaborative.engine.service.generator.BackendGeneratorStrategy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/proyectos/{id}")
public class InteroperabilidadController {

    private final CaseFileService caseFileService;
    private final XmiInteroperabilityService xmiInteroperabilityService;
    private final ImageDiagramImportService imageDiagramImportService;
    private final BackendGeneratorRegistry generatorRegistry;
    private final DiagramaUmlRepository diagramaRepository;

    public InteroperabilidadController(CaseFileService caseFileService,
                                       XmiInteroperabilityService xmiInteroperabilityService,
                                       ImageDiagramImportService imageDiagramImportService,
                                       BackendGeneratorRegistry generatorRegistry,
                                       DiagramaUmlRepository diagramaRepository) {
        this.caseFileService = caseFileService;
        this.xmiInteroperabilityService = xmiInteroperabilityService;
        this.imageDiagramImportService = imageDiagramImportService;
        this.generatorRegistry = generatorRegistry;
        this.diagramaRepository = diagramaRepository;
    }

    @PostMapping(value = "/importar/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importarImagen(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Gemini-Api-Key", required = false) String headerApiKey,
            @RequestParam(value = "apiKey", required = false) String paramApiKey) {
        try {
            String apiKey = (headerApiKey != null && !headerApiKey.isBlank()) ? headerApiKey : paramApiKey;
            var result = imageDiagramImportService.importarDesdeImagen(id, file.getBytes(), file.getContentType(), apiKey);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage(), "message", e.getMessage()));
        }
    }

    @GetMapping("/exportar/case")
    public ResponseEntity<byte[]> exportarCase(@PathVariable Long id) {
        byte[] data = caseFileService.exportarProyectoACase(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"proyecto_" + id + ".case\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }

    @PostMapping("/importar/case")
    public ResponseEntity<String> importarCase(@PathVariable Long id, @RequestBody String jsonContent) {
        caseFileService.importarProyectoDesdeCase(id, jsonContent);
        return ResponseEntity.ok("Proyecto importado exitosamente desde .case");
    }

    @GetMapping("/exportar/xmi")
    public ResponseEntity<byte[]> exportarXmi(@PathVariable Long id) {
        byte[] data = xmiInteroperabilityService.exportarXmi(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"proyecto_" + id + ".xmi\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(data);
    }

    @PostMapping("/importar/xmi")
    public ResponseEntity<String> importarXmi(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            xmiInteroperabilityService.importarXmi(id, file.getInputStream());
            return ResponseEntity.ok("Proyecto importado exitosamente desde XMI");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error al leer el archivo XMI");
        }
    }

    @GetMapping("/generar/backend-zip")
    public ResponseEntity<byte[]> generarBackendZip(@PathVariable Long id, @RequestParam(defaultValue = "SPRING_BOOT_POSTGRES") String tipo) {
        DiagramaUml diagrama = diagramaRepository.findByProyectoId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diagrama no encontrado para el proyecto"));

        BackendGeneratorStrategy strategy = generatorRegistry.getStrategy(tipo);
        byte[] zipData = strategy.generarProyecto(diagrama);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"backend_" + id + ".zip\"")
                .contentType(MediaType.valueOf("application/zip"))
                .body(zipData);
    }

    @GetMapping("/generar/ddl")
    public ResponseEntity<String> generarDdl(@PathVariable Long id, @RequestParam(defaultValue = "SPRING_BOOT_POSTGRES") String tipo) {
        DiagramaUml diagrama = diagramaRepository.findByProyectoId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diagrama no encontrado para el proyecto"));

        BackendGeneratorStrategy strategy = generatorRegistry.getStrategy(tipo);
        String ddl = strategy.generarScriptDdl(diagrama);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(ddl);
    }
}
