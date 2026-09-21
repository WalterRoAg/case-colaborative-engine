package com.casecollaborative.engine.service.generator;

import com.casecollaborative.engine.model.entity.*;
import com.casecollaborative.engine.repository.AtributoRepository;
import com.casecollaborative.engine.repository.ClaseRepository;
import com.casecollaborative.engine.repository.RelacionClaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringBootPostgresGeneratorServiceTest {

    @Mock
    private ClaseRepository claseRepository;

    @Mock
    private AtributoRepository atributoRepository;

    @Mock
    private RelacionClaseRepository relacionRepository;

    private SpringBootPostgresGeneratorService generatorService;

    @BeforeEach
    void setUp() {
        generatorService = new SpringBootPostgresGeneratorService(claseRepository, atributoRepository, relacionRepository);
    }

    @Test
    void testGenerarProyectoZipCompleto() throws Exception {
        Proyecto proyecto = new Proyecto();
        proyecto.setId(100L);
        proyecto.setNombre("SistemaVentas");

        DiagramaUml diagrama = new DiagramaUml();
        diagrama.setId(10L);
        diagrama.setProyecto(proyecto);
        diagrama.setNombre("Diagrama Principal");

        // Clases: Cliente y Factura
        Clase cliente = new Clase();
        cliente.setId(1L);
        cliente.setDiagrama(diagrama);
        cliente.setNombre("Cliente");

        Clase factura = new Clase();
        factura.setId(2L);
        factura.setDiagrama(diagrama);
        factura.setNombre("Factura");

        List<Clase> clases = List.of(cliente, factura);
        when(claseRepository.findByDiagramaId(10L)).thenReturn(clases);

        // Atributos de Cliente
        Atributo cliId = new Atributo();
        cliId.setId(101L);
        cliId.setClase(cliente);
        cliId.setNombre("id");
        cliId.setTipoDato("Long");
        cliId.setEsPk(true);

        Atributo cliNombre = new Atributo();
        cliNombre.setId(102L);
        cliNombre.setClase(cliente);
        cliNombre.setNombre("nombre");
        cliNombre.setTipoDato("String");
        cliNombre.setEsPk(false);

        Atributo cliFechaReg = new Atributo();
        cliFechaReg.setId(103L);
        cliFechaReg.setClase(cliente);
        cliFechaReg.setNombre("fechaRegistro");
        cliFechaReg.setTipoDato("LocalDate");
        cliFechaReg.setEsPk(false);

        when(atributoRepository.findByClaseId(1L)).thenReturn(List.of(cliId, cliNombre, cliFechaReg));

        // Atributos de Factura
        Atributo facId = new Atributo();
        facId.setId(201L);
        facId.setClase(factura);
        facId.setNombre("id");
        facId.setTipoDato("Long");
        facId.setEsPk(true);

        Atributo facMonto = new Atributo();
        facMonto.setId(202L);
        facMonto.setClase(factura);
        facMonto.setNombre("montoTotal");
        facMonto.setTipoDato("BigDecimal");
        facMonto.setEsPk(false);

        Atributo facFechaEmision = new Atributo();
        facFechaEmision.setId(203L);
        facFechaEmision.setClase(factura);
        facFechaEmision.setNombre("fechaEmision");
        facFechaEmision.setTipoDato("LocalDateTime");
        facFechaEmision.setEsPk(false);

        when(atributoRepository.findByClaseId(2L)).thenReturn(List.of(facId, facMonto, facFechaEmision));
        when(relacionRepository.findByDiagramaId(10L)).thenReturn(Collections.emptyList());

        byte[] zipBytes = generatorService.generarProyecto(diagrama);
        assertNotNull(zipBytes);
        assertTrue(zipBytes.length > 0);

        Map<String, String> zipContents = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                byte[] buffer = zis.readAllBytes();
                zipContents.put(entry.getName(), new String(buffer, StandardCharsets.UTF_8));
            }
        }

        // 1. Verificar pom.xml
        assertTrue(zipContents.containsKey("pom.xml"));
        String pom = zipContents.get("pom.xml");
        assertTrue(pom.contains("<java.version>21</java.version>"));
        assertTrue(pom.contains("spring-boot-starter-web"));
        assertTrue(pom.contains("spring-boot-starter-data-jpa"));
        assertTrue(pom.contains("spring-boot-starter-validation"));

        // 2. Verificar application.yml
        assertTrue(zipContents.containsKey("src/main/resources/application.yml"));
        String yml = zipContents.get("src/main/resources/application.yml");
        assertTrue(yml.contains("${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/case_sistemaventas}"));
        assertTrue(yml.contains("${PORT:8081}"));

        // 3. Verificar Entidades y sus Imports
        assertTrue(zipContents.containsKey("src/main/java/com/generated/app/entity/Cliente.java"));
        String clienteEntity = zipContents.get("src/main/java/com/generated/app/entity/Cliente.java");
        assertTrue(clienteEntity.contains("import java.time.LocalDate;"));
        assertTrue(clienteEntity.contains("private LocalDate fechaRegistro;"));

        assertTrue(zipContents.containsKey("src/main/java/com/generated/app/entity/Factura.java"));
        String facturaEntity = zipContents.get("src/main/java/com/generated/app/entity/Factura.java");
        assertTrue(facturaEntity.contains("import java.math.BigDecimal;"));
        assertTrue(facturaEntity.contains("import java.time.LocalDateTime;"));
        assertTrue(facturaEntity.contains("private BigDecimal montoTotal;"));
        assertTrue(facturaEntity.contains("private LocalDateTime fechaEmision;"));

        // 4. Verificar Controladores con 5 métodos CRUD
        assertTrue(zipContents.containsKey("src/main/java/com/generated/app/controller/ClienteController.java"));
        String clienteCtrl = zipContents.get("src/main/java/com/generated/app/controller/ClienteController.java");
        assertTrue(clienteCtrl.contains("@GetMapping\n    public ResponseEntity<List<ClienteResponseDTO>> findAll()"));
        assertTrue(clienteCtrl.contains("@GetMapping(\"/{id}\")\n    public ResponseEntity<ClienteResponseDTO> findById(@PathVariable Long id)"));
        assertTrue(clienteCtrl.contains("@PostMapping\n    public ResponseEntity<ClienteResponseDTO> create(@Valid @RequestBody ClienteRequestDTO dto)"));
        assertTrue(clienteCtrl.contains("@PutMapping(\"/{id}\")\n    public ResponseEntity<ClienteResponseDTO> update(@PathVariable Long id, @Valid @RequestBody ClienteRequestDTO dto)"));
        assertTrue(clienteCtrl.contains("@DeleteMapping(\"/{id}\")\n    public ResponseEntity<Void> deleteById(@PathVariable Long id)"));
        assertTrue(clienteCtrl.contains("HttpStatus.CREATED"));
        assertTrue(clienteCtrl.contains("ResponseEntity.noContent()"));
        assertTrue(clienteCtrl.contains("@CrossOrigin(originPatterns = \"*\")"));

        // 5. Verificar Servicios
        assertTrue(zipContents.containsKey("src/main/java/com/generated/app/service/ClienteService.java"));
        String clienteService = zipContents.get("src/main/java/com/generated/app/service/ClienteService.java");
        assertTrue(clienteService.contains("public List<ClienteResponseDTO> findAll()"));
        assertTrue(clienteService.contains("public Optional<ClienteResponseDTO> findById(Long id)"));
        assertTrue(clienteService.contains("public ClienteResponseDTO create(ClienteRequestDTO dto)"));
        assertTrue(clienteService.contains("public Optional<ClienteResponseDTO> update(Long id, ClienteRequestDTO dto)"));
        assertTrue(clienteService.contains("public boolean deleteById(Long id)"));

        // 6. Verificar DTOs
        assertTrue(zipContents.containsKey("src/main/java/com/generated/app/dto/ClienteRequestDTO.java"));
        String clienteReqDto = zipContents.get("src/main/java/com/generated/app/dto/ClienteRequestDTO.java");
        assertTrue(clienteReqDto.contains("import java.time.LocalDate;"));
        assertTrue(clienteReqDto.contains("public record ClienteRequestDTO("));

        assertTrue(zipContents.containsKey("src/main/java/com/generated/app/dto/FacturaRequestDTO.java"));
        String facturaReqDto = zipContents.get("src/main/java/com/generated/app/dto/FacturaRequestDTO.java");
        assertTrue(facturaReqDto.contains("import java.math.BigDecimal;"));
        assertTrue(facturaReqDto.contains("import java.time.LocalDateTime;"));

        // 7. Verificar DDL
        assertTrue(zipContents.containsKey("src/main/resources/schema.sql"));
        String ddl = zipContents.get("src/main/resources/schema.sql");
        assertTrue(ddl.contains("CREATE TABLE IF NOT EXISTS cliente"));
        assertTrue(ddl.contains("CREATE TABLE IF NOT EXISTS factura"));
        assertTrue(ddl.contains("fecharegistro DATE"));
        assertTrue(ddl.contains("montototal NUMERIC(19, 4)"));
        assertTrue(ddl.contains("fechaemision TIMESTAMP WITHOUT TIME ZONE"));
    }
}
