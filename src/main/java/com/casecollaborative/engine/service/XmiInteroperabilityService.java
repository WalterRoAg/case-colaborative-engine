package com.casecollaborative.engine.service;

import com.casecollaborative.engine.model.entity.*;
import com.casecollaborative.engine.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@Service
public class XmiInteroperabilityService {

    private final DiagramaUmlRepository diagramaRepository;
    private final ClaseRepository claseRepository;
    private final AtributoRepository atributoRepository;
    private final RelacionClaseRepository relacionRepository;
    private final ProyectoRepository proyectoRepository;

    public XmiInteroperabilityService(DiagramaUmlRepository diagramaRepository,
                                      ClaseRepository claseRepository,
                                      AtributoRepository atributoRepository,
                                      RelacionClaseRepository relacionRepository,
                                      ProyectoRepository proyectoRepository) {
        this.diagramaRepository = diagramaRepository;
        this.claseRepository = claseRepository;
        this.atributoRepository = atributoRepository;
        this.relacionRepository = relacionRepository;
        this.proyectoRepository = proyectoRepository;
    }

    @Transactional(readOnly = true)
    public byte[] exportarXmi(Long proyectoId) {
        DiagramaUml diagrama = diagramaRepository.findByProyectoId(proyectoId)
                .orElseThrow(() -> new RuntimeException("Diagrama no encontrado"));

        List<Clase> clases = claseRepository.findByDiagramaId(diagrama.getId());
        List<RelacionClase> relaciones = relacionRepository.findByDiagramaId(diagrama.getId());

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElement("xmi:XMI");
            rootElement.setAttribute("xmi:version", "2.1");
            rootElement.setAttribute("xmlns:xmi", "http://schema.omg.org/spec/XMI/2.1");
            rootElement.setAttribute("xmlns:uml", "http://schema.omg.org/spec/UML/2.1");
            doc.appendChild(rootElement);

            Element docElement = doc.createElement("xmi:Documentation");
            docElement.setAttribute("exporter", "Enterprise Architect");
            docElement.setAttribute("exporterVersion", "6.5");
            rootElement.appendChild(docElement);

            Element modelElement = doc.createElement("uml:Model");
            modelElement.setAttribute("xmi:type", "uml:Model");
            modelElement.setAttribute("name", "EA_Model");
            modelElement.setAttribute("visibility", "public");
            rootElement.appendChild(modelElement);

            Element packagedElement = doc.createElement("packagedElement");
            packagedElement.setAttribute("xmi:type", "uml:Package");
            packagedElement.setAttribute("xmi:id", "EAPK_" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
            packagedElement.setAttribute("name", diagrama.getNombre());
            packagedElement.setAttribute("visibility", "public");
            modelElement.appendChild(packagedElement);

            for (Clase c : clases) {
                Element claseElement = doc.createElement("packagedElement");
                String stereo = c.getEstereotipo() != null ? c.getEstereotipo().toUpperCase() : "CLASS";
                String typeStr = "uml:Class";
                if ("INTERFACE".equals(stereo)) typeStr = "uml:Interface";
                else if ("ENUM".equals(stereo)) typeStr = "uml:Enumeration";
                else if ("DATATYPE".equals(stereo)) typeStr = "uml:DataType";
                else if ("PACKAGE".equals(stereo)) typeStr = "uml:Package";

                claseElement.setAttribute("xmi:type", typeStr);
                claseElement.setAttribute("xmi:id", "EAID_CLS_" + c.getId());
                claseElement.setAttribute("name", c.getNombre());
                claseElement.setAttribute("visibility", c.getVisibilidad() != null ? c.getVisibilidad() : "public");
                packagedElement.appendChild(claseElement);

                List<Atributo> atributos = atributoRepository.findByClaseId(c.getId());
                for (Atributo a : atributos) {
                    Element attrElement = doc.createElement("ownedAttribute");
                    attrElement.setAttribute("xmi:type", "uml:Property");
                    attrElement.setAttribute("xmi:id", "EAID_ATT_" + a.getId());
                    attrElement.setAttribute("name", a.getNombre());
                    attrElement.setAttribute("visibility", a.getVisibilidad() != null ? a.getVisibilidad() : "private");
                    
                    Element typeEl = doc.createElement("type");
                    typeEl.setAttribute("xmi:type", "uml:PrimitiveType");
                    typeEl.setAttribute("href", "http://schema.omg.org/spec/UML/2.1/uml.xml#" + a.getTipoDato());
                    attrElement.appendChild(typeEl);

                    claseElement.appendChild(attrElement);
                }
            }

            for (RelacionClase r : relaciones) {
                if ("GENERALIZACION".equalsIgnoreCase(r.getTipoRelacion())) {
                    NodeList nodes = packagedElement.getChildNodes();
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Node node = nodes.item(i);
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            Element el = (Element) node;
                            if (el.getAttribute("xmi:id").equals("EAID_CLS_" + r.getClaseOrigen().getId())) {
                                Element genElement = doc.createElement("generalization");
                                genElement.setAttribute("xmi:type", "uml:Generalization");
                                genElement.setAttribute("xmi:id", "EAID_GEN_" + r.getId());
                                genElement.setAttribute("general", "EAID_CLS_" + r.getClaseDestino().getId());
                                el.appendChild(genElement);
                                break;
                            }
                        }
                    }
                } else if ("REALIZACION".equalsIgnoreCase(r.getTipoRelacion())) {
                    Element realElement = doc.createElement("packagedElement");
                    realElement.setAttribute("xmi:type", "uml:Realization");
                    realElement.setAttribute("xmi:id", "EAID_REAL_" + r.getId());
                    realElement.setAttribute("client", "EAID_CLS_" + r.getClaseOrigen().getId());
                    realElement.setAttribute("supplier", "EAID_CLS_" + r.getClaseDestino().getId());
                    packagedElement.appendChild(realElement);
                } else if ("DEPENDENCIA".equalsIgnoreCase(r.getTipoRelacion())) {
                    Element depElement = doc.createElement("packagedElement");
                    depElement.setAttribute("xmi:type", "uml:Dependency");
                    depElement.setAttribute("xmi:id", "EAID_DEP_" + r.getId());
                    depElement.setAttribute("client", "EAID_CLS_" + r.getClaseOrigen().getId());
                    depElement.setAttribute("supplier", "EAID_CLS_" + r.getClaseDestino().getId());
                    packagedElement.appendChild(depElement);
                } else {
                    Element assocElement = doc.createElement("packagedElement");
                    assocElement.setAttribute("xmi:type", "uml:Association");
                    assocElement.setAttribute("xmi:id", "EAID_ASSOC_" + r.getId());
                    if (r.getNombre() != null && !r.getNombre().isEmpty()) {
                        assocElement.setAttribute("name", r.getNombre());
                    }

                    Element end1 = doc.createElement("ownedEnd");
                    end1.setAttribute("type", "EAID_CLS_" + r.getClaseOrigen().getId());
                    if (r.getCardinalidadOrigen() != null) {
                        Element lower1 = doc.createElement("lowerValue");
                        lower1.setAttribute("value", r.getCardinalidadOrigen());
                        end1.appendChild(lower1);
                    }
                    assocElement.appendChild(end1);

                    Element end2 = doc.createElement("ownedEnd");
                    end2.setAttribute("type", "EAID_CLS_" + r.getClaseDestino().getId());
                    if ("COMPOSICION".equalsIgnoreCase(r.getTipoRelacion())) {
                        end2.setAttribute("aggregation", "composite");
                    } else if ("AGREGACION".equalsIgnoreCase(r.getTipoRelacion())) {
                        end2.setAttribute("aggregation", "shared");
                    }
                    if (r.getCardinalidadDestino() != null) {
                        Element lower2 = doc.createElement("lowerValue");
                        lower2.setAttribute("value", r.getCardinalidadDestino());
                        end2.appendChild(lower2);
                    }
                    assocElement.appendChild(end2);

                    packagedElement.appendChild(assocElement);
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(bos);

            transformer.transform(source, result);
            return bos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al exportar XMI", e);
        }
    }

    @Transactional
    public void importarXmi(Long proyectoId, InputStream xmiInputStream) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        DiagramaUml diagrama = diagramaRepository.findByProyectoId(proyectoId)
                .orElseGet(() -> {
                    DiagramaUml d = new DiagramaUml();
                    d.setProyecto(proyecto);
                    d.setNombre(proyecto.getNombre());
                    d.setVersion(1);
                    d.setZoomCanvas(1.0);
                    d.setPanX(0.0);
                    d.setPanY(0.0);
                    return diagramaRepository.save(d);
                });

        // Limpiar elementos existentes del diagrama
        List<RelacionClase> relsAnteriores = relacionRepository.findByDiagramaId(diagrama.getId());
        relacionRepository.deleteAll(relsAnteriores);

        List<Clase> clasesAnteriores = claseRepository.findByDiagramaId(diagrama.getId());
        for (Clase c : clasesAnteriores) {
            atributoRepository.deleteAll(atributoRepository.findByClaseId(c.getId()));
        }
        claseRepository.deleteAll(clasesAnteriores);

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(false);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmiInputStream);
            doc.getDocumentElement().normalize();

            Map<String, Clase> xmiIdToClaseMap = new HashMap<>();
            Map<String, String> propertyToClassMap = new HashMap<>();
            Map<String, String> propertyToTargetTypeMap = new HashMap<>();

            // 0. PASO PREVIO: Parsear elementos y coordenadas específicas del Diagrama si existen en el XMI de EA
            Map<String, double[]> diagramElementPosMap = new LinkedHashMap<>();
            NodeList diagList = doc.getElementsByTagName("diagram");
            for (int d = 0; d < diagList.getLength(); d++) {
                Node diagNode = diagList.item(d);
                if (diagNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element diagEl = (Element) diagNode;
                    NodeList diagElements = diagEl.getElementsByTagName("element");
                    for (int de = 0; de < diagElements.getLength(); de++) {
                        Node deNode = diagElements.item(de);
                        if (deNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element dEl = (Element) deNode;
                            String subj = getAttributeValue(dEl, "subject", "xmi:idref", "idref");
                            String geom = dEl.getAttribute("geometry");
                            if (subj != null && !subj.isEmpty()) {
                                double left = 100;
                                double top = 100;
                                if (geom != null && !geom.isEmpty()) {
                                    for (String part : geom.split(";")) {
                                        String[] kv = part.split("=");
                                        if (kv.length == 2) {
                                            String k = kv[0].trim().toLowerCase();
                                            String v = kv[1].trim();
                                            try {
                                                if (k.equals("left")) left = Math.abs(Double.parseDouble(v));
                                                if (k.equals("top")) top = Math.abs(Double.parseDouble(v));
                                            } catch (NumberFormatException ignored) {}
                                        }
                                    }
                                }
                                diagramElementPosMap.put(subj, new double[]{left, top});
                            }
                        }
                    }
                }
            }

            // Layout en cuadrícula fallback si no hay coordenadas en el diagrama
            double startX = 80.0;
            double startY = 80.0;
            double colSpacing = 240.0;
            double rowSpacing = 180.0;
            int cols = 4;
            int classIndex = 0;

            // 1. PRIMER PASO: Buscar y crear clases, interfaces, enums del modelo real
            NodeList allElements = doc.getElementsByTagName("*");
            for (int i = 0; i < allElements.getLength(); i++) {
                Node node = allElements.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) node;
                    String tagName = el.getTagName();
                    String xmiType = getAttributeValue(el, "xmi:type", "type");
                    String xmiId = getAttributeValue(el, "xmi:id", "id");

                    boolean isPackage = "uml:Package".equalsIgnoreCase(xmiType) || 
                                        "Package".equalsIgnoreCase(xmiType) || 
                                        "package".equalsIgnoreCase(tagName);

                    // Descartar paquetes (carpetas/namespaces no son tablas en el lienzo)
                    if (isPackage) {
                        continue;
                    }

                    boolean isClassElement = "packagedElement".equalsIgnoreCase(tagName) || "element".equalsIgnoreCase(tagName);
                    boolean isClassType = "uml:Class".equalsIgnoreCase(xmiType) ||
                                          "uml:Interface".equalsIgnoreCase(xmiType) ||
                                          "uml:Enumeration".equalsIgnoreCase(xmiType) ||
                                          "uml:DataType".equalsIgnoreCase(xmiType) ||
                                          "Class".equalsIgnoreCase(xmiType) ||
                                          "Interface".equalsIgnoreCase(xmiType);

                    if (isClassElement && isClassType && xmiId != null && !xmiId.isEmpty()) {
                        String name = el.getAttribute("name");
                        if (name == null || name.trim().isEmpty()) {
                            name = getChildElementValue(el, "properties", "name");
                        }

                        // Ignorar catálogos internos de tipos de Enterprise Architect y nombres de paquetes
                        String nameLower = (name != null) ? name.toLowerCase() : "";
                        if (nameLower.startsWith("ea_") ||
                            nameLower.contains("primitivetypes") ||
                            nameLower.contains("types_package") ||
                            nameLower.contains("type_package") ||
                            nameLower.equals("earootclass")) {
                            continue;
                        }

                        // Si el XMI contiene la sección de diagramas de EA, incluir solo los elementos del diagrama activo
                        if (!diagramElementPosMap.isEmpty() && !diagramElementPosMap.containsKey(xmiId)) {
                            continue;
                        }

                        if (name == null || name.trim().isEmpty()) {
                            name = "Clase_" + (classIndex + 1);
                        }

                        String stereo = "CLASS";
                        if (xmiType != null && xmiType.toLowerCase().contains("interface")) stereo = "INTERFACE";
                        else if (xmiType != null && xmiType.toLowerCase().contains("enum")) stereo = "ENUM";
                        else if (xmiType != null && xmiType.toLowerCase().contains("datatype")) stereo = "DATATYPE";

                        String visibility = el.getAttribute("visibility");
                        if (visibility == null || visibility.isEmpty()) visibility = "public";

                        // Asignar posición: del diagrama de EA o calculada en cuadrícula
                        double posX;
                        double posY;
                        if (diagramElementPosMap.containsKey(xmiId)) {
                            double[] coords = diagramElementPosMap.get(xmiId);
                            posX = Math.max(40.0, coords[0]);
                            posY = Math.max(40.0, coords[1]);
                        } else {
                            int row = classIndex / cols;
                            int col = classIndex % cols;
                            posX = startX + (col * colSpacing);
                            posY = startY + (row * rowSpacing);
                        }

                        Clase clase = new Clase();
                        clase.setDiagrama(diagrama);
                        clase.setNombre(name);
                        clase.setEstereotipo(stereo);
                        clase.setVisibilidad(visibility);
                        clase.setPosX(posX);
                        clase.setPosY(posY);
                        clase = claseRepository.save(clase);

                        xmiIdToClaseMap.put(xmiId, clase);
                        classIndex++;

                        // Parsear Atributos dentro de la clase
                        NodeList childNodes = el.getChildNodes();
                        int attrOrder = 0;
                        for (int j = 0; j < childNodes.getLength(); j++) {
                            Node child = childNodes.item(j);
                            if (child.getNodeType() == Node.ELEMENT_NODE) {
                                Element childEl = (Element) child;
                                String childTag = childEl.getTagName();
                                String propType = getAttributeValue(childEl, "xmi:type", "type");
                                String propId = getAttributeValue(childEl, "xmi:id", "id");

                                if ("ownedAttribute".equalsIgnoreCase(childTag) || "attribute".equalsIgnoreCase(childTag) || "uml:Property".equalsIgnoreCase(propType)) {
                                    String attrName = childEl.getAttribute("name");
                                    String assocId = childEl.getAttribute("association");

                                    // Si es una propiedad con type o target ref
                                    String targetRef = null;
                                    NodeList typeNodes = childEl.getElementsByTagName("type");
                                    if (typeNodes.getLength() > 0) {
                                        Element typeNode = (Element) typeNodes.item(0);
                                        targetRef = getAttributeValue(typeNode, "xmi:idref", "idref");
                                    }
                                    if (propId != null) {
                                        propertyToClassMap.put(propId, xmiId);
                                        if (targetRef != null) propertyToTargetTypeMap.put(propId, targetRef);
                                    }

                                    // Si es un atributo con nombre real
                                    if (attrName != null && !attrName.trim().isEmpty()) {
                                        String tipoDato = parseDataType(childEl);
                                        String attrVis = childEl.getAttribute("visibility");
                                        if (attrVis == null || attrVis.isEmpty()) attrVis = "private";

                                        boolean isPk = attrName.equalsIgnoreCase("id") || attrName.toLowerCase().endsWith("_id");

                                        Atributo attr = new Atributo();
                                        attr.setClase(clase);
                                        attr.setNombre(attrName);
                                        attr.setTipoDato(tipoDato);
                                        attr.setVisibilidad(attrVis);
                                        attr.setEsPk(isPk);
                                        attr.setOrden(attrOrder++);
                                        atributoRepository.save(attr);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. SEGUNDO PASO: Parsear Generalizaciones (Herencias)
            NodeList genNodes = doc.getElementsByTagName("generalization");
            for (int i = 0; i < genNodes.getLength(); i++) {
                Node node = genNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element genEl = (Element) node;
                    String targetId = getAttributeValue(genEl, "general", "target");
                    if (targetId == null && genEl.getElementsByTagName("general").getLength() > 0) {
                        Element gEl = (Element) genEl.getElementsByTagName("general").item(0);
                        targetId = getAttributeValue(gEl, "xmi:idref", "idref");
                    }

                    Node parentNode = genEl.getParentNode();
                    String sourceId = null;
                    if (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
                        sourceId = getAttributeValue((Element) parentNode, "xmi:id", "id");
                    }

                    if (sourceId != null && targetId != null) {
                        Clase cOrig = xmiIdToClaseMap.get(sourceId);
                        Clase cDest = xmiIdToClaseMap.get(targetId);
                        if (cOrig != null && cDest != null) {
                            crearRelacionSiNoExiste(diagrama, cOrig, cDest, "GENERALIZACION", "", "1", "1");
                        }
                    }
                }
            }

            // 3. TERCER PASO: Parsear Realizaciones, Dependencias y Usos
            for (int i = 0; i < allElements.getLength(); i++) {
                Node node = allElements.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) node;
                    String xmiType = getAttributeValue(el, "xmi:type", "type");
                    String tag = el.getTagName();

                    boolean isRealization = "uml:Realization".equalsIgnoreCase(xmiType) || "realization".equalsIgnoreCase(tag) || "interfaceRealization".equalsIgnoreCase(tag);
                    boolean isDependency = "uml:Dependency".equalsIgnoreCase(xmiType) || "uml:Usage".equalsIgnoreCase(xmiType) || "dependency".equalsIgnoreCase(tag);

                    if (isRealization || isDependency) {
                        String clientId = getAttributeValue(el, "client", "source");
                        String supplierId = getAttributeValue(el, "supplier", "target");

                        if (clientId == null && el.getElementsByTagName("client").getLength() > 0) {
                            clientId = getAttributeValue((Element) el.getElementsByTagName("client").item(0), "xmi:idref", "idref");
                        }
                        if (supplierId == null && el.getElementsByTagName("supplier").getLength() > 0) {
                            supplierId = getAttributeValue((Element) el.getElementsByTagName("supplier").item(0), "xmi:idref", "idref");
                        }

                        if (clientId != null && supplierId != null) {
                            Clase cOrig = xmiIdToClaseMap.get(clientId);
                            Clase cDest = xmiIdToClaseMap.get(supplierId);
                            if (cOrig != null && cDest != null) {
                                String tipoRel = isRealization ? "REALIZACION" : "DEPENDENCIA";
                                crearRelacionSiNoExiste(diagrama, cOrig, cDest, tipoRel, el.getAttribute("name"), "1", "1");
                            }
                        }
                    }
                }
            }

            // 4. CUARTO PASO: Parsear Asociaciones UML Estándar
            for (int i = 0; i < allElements.getLength(); i++) {
                Node node = allElements.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) node;
                    String xmiType = getAttributeValue(el, "xmi:type", "type");

                    if ("uml:Association".equalsIgnoreCase(xmiType) || "Association".equalsIgnoreCase(el.getTagName())) {
                        String relName = el.getAttribute("name");
                        List<Element> ownedEnds = new ArrayList<>();
                        List<String> memberEndIds = new ArrayList<>();

                        NodeList children = el.getChildNodes();
                        for (int k = 0; k < children.getLength(); k++) {
                            if (children.item(k).getNodeType() == Node.ELEMENT_NODE) {
                                Element ch = (Element) children.item(k);
                                if ("ownedEnd".equalsIgnoreCase(ch.getTagName())) {
                                    ownedEnds.add(ch);
                                } else if ("memberEnd".equalsIgnoreCase(ch.getTagName())) {
                                    String idref = getAttributeValue(ch, "xmi:idref", "idref");
                                    if (idref != null) memberEndIds.add(idref);
                                }
                            }
                        }

                        Clase cOrig = null;
                        Clase cDest = null;
                        String cardOrig = "1";
                        String cardDest = "1";
                        String tipoRel = "ASOCIACION";

                        if (ownedEnds.size() >= 2) {
                            Element end1 = ownedEnds.get(0);
                            Element end2 = ownedEnds.get(1);

                            String t1 = getAttributeValue(end1, "type", "xmi:idref");
                            String t2 = getAttributeValue(end2, "type", "xmi:idref");

                            cOrig = xmiIdToClaseMap.get(t1);
                            cDest = xmiIdToClaseMap.get(t2);

                            String agg1 = end1.getAttribute("aggregation");
                            String agg2 = end2.getAttribute("aggregation");
                            if ("composite".equalsIgnoreCase(agg1) || "composite".equalsIgnoreCase(agg2)) {
                                tipoRel = "COMPOSICION";
                            } else if ("shared".equalsIgnoreCase(agg1) || "shared".equalsIgnoreCase(agg2)) {
                                tipoRel = "AGREGACION";
                            }
                        } else if (memberEndIds.size() >= 2) {
                            String prop1 = memberEndIds.get(0);
                            String prop2 = memberEndIds.get(1);

                            String class1Id = propertyToClassMap.get(prop1);
                            String class2Id = propertyToClassMap.get(prop2);

                            if (class1Id != null && class2Id != null) {
                                cOrig = xmiIdToClaseMap.get(class1Id);
                                cDest = xmiIdToClaseMap.get(class2Id);
                            }
                        }

                        if (cOrig != null && cDest != null) {
                            crearRelacionSiNoExiste(diagrama, cOrig, cDest, tipoRel, relName, cardOrig, cardDest);
                        }
                    }
                }
            }

            // 5. QUINTO PASO: Parsear Conectores Especiales de Enterprise Architect (<connector>)
            NodeList connectorNodes = doc.getElementsByTagName("connector");
            for (int i = 0; i < connectorNodes.getLength(); i++) {
                Node node = connectorNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element connEl = (Element) node;

                    String srcId = null;
                    String dstId = null;
                    String cardOrig = "1";
                    String cardDest = "1";
                    String eaType = "Association";
                    String subtype = "";
                    String name = "";

                    NodeList srcList = connEl.getElementsByTagName("source");
                    if (srcList.getLength() > 0) {
                        Element s = (Element) srcList.item(0);
                        srcId = getAttributeValue(s, "xmi:idref", "idref");
                        NodeList mult = s.getElementsByTagName("multiplicity");
                        if (mult.getLength() > 0) cardOrig = ((Element) mult.item(0)).getAttribute("value");
                    }

                    NodeList dstList = connEl.getElementsByTagName("target");
                    if (dstList.getLength() > 0) {
                        Element t = (Element) dstList.item(0);
                        dstId = getAttributeValue(t, "xmi:idref", "idref");
                        NodeList mult = t.getElementsByTagName("multiplicity");
                        if (mult.getLength() > 0) cardDest = ((Element) mult.item(0)).getAttribute("value");
                    }

                    NodeList propList = connEl.getElementsByTagName("properties");
                    if (propList.getLength() > 0) {
                        Element p = (Element) propList.item(0);
                        if (p.getAttribute("ea_type") != null) eaType = p.getAttribute("ea_type");
                        if (p.getAttribute("subtype") != null) subtype = p.getAttribute("subtype");
                        if (p.getAttribute("name") != null) name = p.getAttribute("name");
                    }

                    if (srcId != null && dstId != null) {
                        Clase cOrig = xmiIdToClaseMap.get(srcId);
                        Clase cDest = xmiIdToClaseMap.get(dstId);

                        if (cOrig != null && cDest != null) {
                            String tipoRel = "ASOCIACION";
                            if ("Generalization".equalsIgnoreCase(eaType)) {
                                tipoRel = "GENERALIZACION";
                            } else if ("Realisation".equalsIgnoreCase(eaType) || "Realization".equalsIgnoreCase(eaType)) {
                                tipoRel = "REALIZACION";
                            } else if ("Dependency".equalsIgnoreCase(eaType) || "Usage".equalsIgnoreCase(eaType)) {
                                tipoRel = "DEPENDENCIA";
                            } else if ("Aggregation".equalsIgnoreCase(eaType) || "Shared".equalsIgnoreCase(subtype)) {
                                tipoRel = "AGREGACION";
                            } else if ("Composition".equalsIgnoreCase(eaType) || "Composite".equalsIgnoreCase(subtype)) {
                                tipoRel = "COMPOSICION";
                            }

                            crearRelacionSiNoExiste(diagrama, cOrig, cDest, tipoRel, name, cardOrig, cardDest);
                        }
                    }
                }
            }

            diagramaRepository.save(diagrama);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al importar archivo XMI de Enterprise Architect: " + e.getMessage(), e);
        }
    }

    private void crearRelacionSiNoExiste(DiagramaUml diagrama, Clase cOrig, Clase cDest, String tipoRel, String nombre, String cardOrig, String cardDest) {
        List<RelacionClase> existentes = relacionRepository.findByDiagramaId(diagrama.getId());
        boolean yaExiste = existentes.stream().anyMatch(r ->
                r.getClaseOrigen().getId().equals(cOrig.getId()) &&
                r.getClaseDestino().getId().equals(cDest.getId()) &&
                r.getTipoRelacion().equalsIgnoreCase(tipoRel)
        );

        if (!yaExiste) {
            RelacionClase rel = new RelacionClase();
            rel.setDiagrama(diagrama);
            rel.setClaseOrigen(cOrig);
            rel.setClaseDestino(cDest);
            rel.setTipoRelacion(tipoRel);
            rel.setNombre(nombre != null ? nombre : "");
            rel.setCardinalidadOrigen(cardOrig != null && !cardOrig.isEmpty() ? cardOrig : "1");
            rel.setCardinalidadDestino(cardDest != null && !cardDest.isEmpty() ? cardDest : "1");
            relacionRepository.save(rel);
        }
    }

    private String getAttributeValue(Element el, String... attrNames) {
        for (String attr : attrNames) {
            if (el.hasAttribute(attr) && !el.getAttribute(attr).isEmpty()) {
                return el.getAttribute(attr);
            }
        }
        return null;
    }

    private String getChildElementValue(Element el, String childTag, String attrName) {
        NodeList list = el.getElementsByTagName(childTag);
        if (list.getLength() > 0) {
            Element ch = (Element) list.item(0);
            return ch.getAttribute(attrName);
        }
        return null;
    }

    private String parseDataType(Element propElement) {
        NodeList typeNodes = propElement.getElementsByTagName("type");
        if (typeNodes.getLength() > 0) {
            Element typeEl = (Element) typeNodes.item(0);
            String href = typeEl.getAttribute("href");
            if (href != null && href.contains("#")) {
                return href.substring(href.lastIndexOf("#") + 1);
            }
            String name = typeEl.getAttribute("name");
            if (name != null && !name.isEmpty()) return name;
        }

        NodeList propNodes = propElement.getElementsByTagName("properties");
        if (propNodes.getLength() > 0) {
            Element pEl = (Element) propNodes.item(0);
            String type = pEl.getAttribute("type");
            if (type != null && !type.isEmpty()) return type;
        }

        String typeAttr = propElement.getAttribute("type");
        if (typeAttr != null && !typeAttr.isEmpty()) return typeAttr;

        return "String";
    }
}
