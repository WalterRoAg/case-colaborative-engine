/**
 * CASE Collaborative Engine - Voice Assistant & Natural Language Voice Modeler
 * Procesa comandos de voz en español para modelado UML colaborativo y generación de Backend.
 */

class VoiceCommandParser {
    static normalizeType(rawType) {
        if (!rawType) return 'String';
        const t = rawType.toLowerCase().trim();
        if (['string', 'texto', 'cadena', 'nombre', 'correo', 'email', 'direccion', 'descripcion', 'titulo', 'telefono', 'varchar', 'str'].includes(t)) return 'String';
        if (['int', 'entero', 'integer', 'numero', 'cantidad', 'edad', 'stock', 'año', 'anio', 'mes', 'dia', 'num'].includes(t)) return 'Integer';
        if (['long', 'id', 'identificador', 'codigo', 'bigint'].includes(t)) return 'Long';
        if (['double', 'float', 'decimal', 'monto', 'precio', 'total', 'saldo', 'costo', 'importe', 'valor', 'subtotal'].includes(t)) return 'Double';
        if (['boolean', 'booleano', 'activo', 'estado', 'logico', 'flag', 'habilitado'].includes(t)) return 'Boolean';
        if (['date', 'fecha', 'timestamp', 'hora', 'fechahora', 'datetime'].includes(t)) return 'Date';
        if (['byte', 'binario', 'archivo', 'imagen', 'foto', 'blob'].includes(t)) return 'byte[]';
        return rawType.charAt(0).toUpperCase() + rawType.slice(1);
    }

    static cleanText(text) {
        return text
            .replace(/[.,;:¿?¡!]/g, ' ')
            .replace(/\s+/g, ' ')
            .trim();
    }

    static parse(rawText) {
        if (!rawText) return null;
        const text = this.cleanText(rawText);
        const lower = text.toLowerCase();

        // 1. IMPORTAR IMAGEN / FOTO
        if (lower.includes('importar imagen') || lower.includes('importar foto') || lower.includes('escanear foto') ||
            lower.includes('escanear imagen') || lower.includes('analizar imagen') || lower.includes('analizar foto') ||
            lower.includes('subir foto') || lower.includes('cargar imagen') || lower.includes('foto a uml')) {
            return {
                type: 'IMPORT_IMAGE'
            };
        }

        // 2. GENERAR BACKEND / CODIGO / ZIP / DASHBOARD / SOFTWARE
        if (lower.includes('generar backend') || lower.includes('crear backend') || lower.includes('generar codigo') || 
            lower.includes('ver codigo') || lower.includes('descargar zip') || lower.includes('descargar backend') ||
            lower.includes('exportar backend') || lower.includes('generar software') || lower.includes('crear software') ||
            lower.includes('crear dashboard') || lower.includes('generar dashboard')) {
            return {
                type: 'GENERATE_BACKEND'
            };
        }

        // 3. GUARDAR AVANCE / DIAGRAMA
        if (lower.includes('guardar avance') || lower.includes('guardar diagrama') || lower.includes('guardar proyecto') || lower.includes('guardar cambios')) {
            return {
                type: 'SAVE_DIAGRAM'
            };
        }

        // 3. SELECCIONAR / DESELECCIONAR TODO
        if (lower.includes('seleccionar todo') || lower.includes('seleccionar todas') || lower.includes('selecciona todo')) {
            return {
                type: 'SELECT_ALL'
            };
        }
        if (lower.includes('deseleccionar todo') || lower.includes('deseleccionar todas') || lower.includes('limpiar seleccion')) {
            return {
                type: 'DESELECT_ALL'
            };
        }

        // 4. ELIMINAR ATRIBUTO DE UNA CLASE/TABLA:
        // Ejemplos: "eliminar atributo telefono de Cliente", "borrar el campo total en Compra", "en Cliente eliminar atributo correo"
        const delAttrMatch1 = text.match(/(?:eliminar|borrar|quitar)\s+(?:el\s+)?(?:atributo|campo)\s+([A-Za-z0-9_]+)\s+(?:de|en|desde)\s+(?:la\s+)?(?:clase|tabla|entidad)?\s*([A-Za-z0-9_]+)/i);
        if (delAttrMatch1) {
            return {
                type: 'DELETE_ATTRIBUTE',
                attrName: delAttrMatch1[1].trim(),
                className: delAttrMatch1[2].trim()
            };
        }
        const delAttrMatch2 = text.match(/(?:en|de)\s+(?:la\s+)?(?:clase|tabla|entidad)?\s*([A-Za-z0-9_]+)\s+(?:eliminar|borrar|quitar)\s+(?:el\s+)?(?:atributo|campo)\s+([A-Za-z0-9_]+)/i);
        if (delAttrMatch2) {
            return {
                type: 'DELETE_ATTRIBUTE',
                className: delAttrMatch2[1].trim(),
                attrName: delAttrMatch2[2].trim()
            };
        }

        // 5. ELIMINAR RELACIÓN ENTRE CLASES:
        // Ejemplos: "eliminar relacion entre Cliente y Compra", "borrar conexion de Pedido con Producto", "desconectar Cliente de Compra"
        const delRelMatch1 = text.match(/(?:eliminar|borrar|quitar)\s+(?:la\s+)?(?:relacion|conexion|enlace|vinculo)\s+(?:entre|de)\s+([A-Za-z0-9_]+)\s+(?:y|con|a|hacia)\s+([A-Za-z0-9_]+)/i);
        if (delRelMatch1) {
            return {
                type: 'DELETE_RELATION',
                sourceClass: delRelMatch1[1].trim(),
                targetClass: delRelMatch1[2].trim()
            };
        }
        const delRelMatch2 = text.match(/(?:desconectar|desvincular|desasociar)\s+([A-Za-z0-9_]+)\s+(?:de|con|y)\s+([A-Za-z0-9_]+)/i);
        if (delRelMatch2) {
            return {
                type: 'DELETE_RELATION',
                sourceClass: delRelMatch2[1].trim(),
                targetClass: delRelMatch2[2].trim()
            };
        }

        // 6. ELIMINAR CLASES (LOTE O INDIVIDUAL):
        // Ejemplos: "eliminar tablas Cliente, Producto y Compra", "eliminar clase Pedido"
        const delClassesBatch = text.match(/(?:eliminar|borrar|quitar)\s+(?:las\s+)?(?:clases|tablas|entidades)\s+([A-Za-z0-9_\s,y]+)/i);
        if (delClassesBatch && (delClassesBatch[1].includes(',') || delClassesBatch[1].includes(' y '))) {
            const rawNames = delClassesBatch[1]
                .replace(/\s+y\s+/gi, ',')
                .split(',')
                .map(n => n.trim())
                .filter(n => n.length > 0);
            if (rawNames.length > 0) {
                return {
                    type: 'DELETE_CLASSES_BATCH',
                    classNames: rawNames
                };
            }
        }
        const delClassMatch = text.match(/(?:eliminar|borrar|quitar)\s+(?:la\s+)?(?:clase|tabla|entidad)\s+([A-Za-z0-9_]+)/i);
        if (delClassMatch) {
            return {
                type: 'DELETE_CLASS',
                className: delClassMatch[1].trim()
            };
        }

        // 7. CREAR CLASES EN LOTE (Batch): "crear clases Cliente, Producto, Compra y NotaDeVenta"
        const batchMatch = text.match(/(?:crear|agregar|añadir|generar)\s+(?:las\s+)?(?:clases|tablas|entidades)\s+([A-Za-z0-9_\s,y]+)/i);
        if (batchMatch && (batchMatch[1].includes(',') || batchMatch[1].includes(' y '))) {
            const rawNames = batchMatch[1]
                .replace(/\s+y\s+/gi, ',')
                .split(',')
                .map(n => n.trim())
                .filter(n => n.length > 0);
            
            if (rawNames.length > 1) {
                return {
                    type: 'CREATE_CLASSES_BATCH',
                    classNames: rawNames
                };
            }
        }

        // 8. CREAR CLASE / TABLA CON ATRIBUTOS: "crear clase Cliente con id entero, nombre texto y correo string"
        const createClassMatch = text.match(/(?:crear|agregar|añadir|nueva)\s+(?:clase|tabla|entidad|interfaz|enumeracion)\s+([A-Za-z0-9_]+)(?:\s+(?:con\s+(?:atributos?|campos?))?\s+(.+))?/i);
        if (createClassMatch) {
            const className = createClassMatch[1].trim();
            const rest = createClassMatch[2] ? createClassMatch[2].trim() : '';
            
            let stereotype = 'CLASS';
            if (lower.includes('interfaz')) stereotype = 'INTERFACE';
            if (lower.includes('enumeracion') || lower.includes('enum')) stereotype = 'ENUM';

            const attributes = [];
            if (rest) {
                const attrChunks = rest
                    .replace(/\s+y\s+/gi, ',')
                    .replace(/(?:con\s+)?atributos?\s+/gi, '')
                    .replace(/(?:con\s+)?campos?\s+/gi, '')
                    .split(',');

                attrChunks.forEach(chunk => {
                    const cleanChunk = chunk.trim();
                    if (!cleanChunk) return;
                    const parts = cleanChunk.replace(/\s+de\s+tipo\s+/i, ' ').replace(/\s+tipo\s+/i, ' ').split(/\s+/);
                    if (parts.length >= 2) {
                        attributes.push({
                            nombre: parts[0],
                            tipoDato: VoiceCommandParser.normalizeType(parts[1]),
                            esPk: lower.includes('clave primaria') || lower.includes(' pk ') || parts[0].toLowerCase() === 'id'
                        });
                    } else if (parts.length === 1 && parts[0]) {
                        attributes.push({
                            nombre: parts[0],
                            tipoDato: 'String',
                            esPk: parts[0].toLowerCase() === 'id'
                        });
                    }
                });
            }

            return {
                type: 'CREATE_CLASS',
                className: className,
                attributes: attributes,
                stereotype: stereotype
            };
        }

        // 9. AGREGAR ATRIBUTO A CLASE EXISTENTE:
        // Formato A: "agregar atributo total Double a la clase Compra"
        const addAttrMatch1 = text.match(/(?:agregar|añadir|poner|crear|insertar)\s+(?:el\s+)?(?:atributo|campo)\s+([A-Za-z0-9_]+)(?:\s+(?:de\s+tipo\s+|tipo\s+)?([A-Za-z0-9_]+))?\s*(?:a|en|para)\s+(?:la\s+)?(?:clase|tabla|entidad)?\s*([A-Za-z0-9_]+)/i);
        if (addAttrMatch1) {
            return {
                type: 'ADD_ATTRIBUTE',
                attrName: addAttrMatch1[1].trim(),
                attrType: VoiceCommandParser.normalizeType(addAttrMatch1[2] || 'String'),
                className: addAttrMatch1[3].trim()
            };
        }
        // Formato B: "agregar a Cliente el atributo telefono String"
        const addAttrMatch2 = text.match(/(?:agregar|añadir|poner|crear|insertar)\s+(?:a|en|para)\s+(?:la\s+)?(?:clase|tabla|entidad)?\s*([A-Za-z0-9_]+)\s+(?:el\s+)?(?:atributo|campo)\s+([A-Za-z0-9_]+)(?:\s+(?:de\s+tipo\s+|tipo\s+)?([A-Za-z0-9_]+))?/i);
        if (addAttrMatch2) {
            return {
                type: 'ADD_ATTRIBUTE',
                className: addAttrMatch2[1].trim(),
                attrName: addAttrMatch2[2].trim(),
                attrType: VoiceCommandParser.normalizeType(addAttrMatch2[3] || 'String')
            };
        }

        // 10. HERENCIA / GENERALIZACION: "Cliente hereda de Persona" / "crear herencia de Alumno a Persona"
        const genMatch = text.match(/(?:hacer\s+que\s+)?([A-Za-z0-9_]+)\s+(?:herede|extienda)\s+de\s+([A-Za-z0-9_]+)/i) ||
                         text.match(/(?:crear\s+herencia|generalizacion)\s+(?:de|entre)\s+([A-Za-z0-9_]+)\s+(?:a|con|hacia)\s+([A-Za-z0-9_]+)/i);
        if (genMatch) {
            return {
                type: 'CONNECT_CLASSES',
                sourceClass: genMatch[1].trim(),
                targetClass: genMatch[2].trim(),
                relType: 'GENERALIZACION'
            };
        }

        // 11. COMPOSICION / AGREGACION / DEPENDENCIA / ASOCIACION CON PALABRAS CLAVE:
        const compMatch = text.match(/(?:crear\s+composicion|componer)\s+(?:de|entre)\s+([A-Za-z0-9_]+)\s+(?:con|y|a)\s+([A-Za-z0-9_]+)/i);
        if (compMatch) {
            return {
                type: 'CONNECT_CLASSES',
                sourceClass: compMatch[1].trim(),
                targetClass: compMatch[2].trim(),
                relType: 'COMPOSICION'
            };
        }
        const agregMatch = text.match(/(?:crear\s+agregacion|agregar)\s+(?:de|entre)\s+([A-Za-z0-9_]+)\s+(?:con|y|a)\s+([A-Za-z0-9_]+)/i);
        if (agregMatch) {
            return {
                type: 'CONNECT_CLASSES',
                sourceClass: agregMatch[1].trim(),
                targetClass: agregMatch[2].trim(),
                relType: 'AGREGACION'
            };
        }

        // 12. CARDINALIDAD UNO A MUCHOS: "Cliente tiene muchas Compras" / "Factura contiene muchos Detalles"
        const oneToManyMatch = text.match(/([A-Za-z0-9_]+)\s+(?:tiene|contiene|posee)\s+(?:muchos?|varios?|muchas?|varias?)\s+([A-Za-z0-9_]+)/i);
        if (oneToManyMatch) {
            return {
                type: 'CONNECT_CLASSES',
                sourceClass: oneToManyMatch[1].trim(),
                targetClass: oneToManyMatch[2].trim(),
                relType: 'ASOCIACION',
                cardOrig: '1',
                cardDest: '*'
            };
        }

        // 13. RELACION / ASOCIACION GENERAL: "conectar Cliente con Compra" / "relacionar NotaDeVenta con Producto"
        const connectMatch = text.match(/(?:asociar|relacionar|conectar|unir|enlazar)\s+([A-Za-z0-9_]+)\s+(?:con|y|a)\s+([A-Za-z0-9_]+)/i);
        if (connectMatch) {
            let relType = 'ASOCIACION';
            if (lower.includes('composicion')) relType = 'COMPOSICION';
            else if (lower.includes('agregacion')) relType = 'AGREGACION';
            else if (lower.includes('dependencia')) relType = 'DEPENDENCIA';
            else if (lower.includes('realizacion')) relType = 'REALIZACION';
            else if (lower.includes('dirigida')) relType = 'ASOCIACION_DIRIGIDA';

            return {
                type: 'CONNECT_CLASSES',
                sourceClass: connectMatch[1].trim(),
                targetClass: connectMatch[2].trim(),
                relType: relType
            };
        }

        return null;
    }
}

class VoiceAssistant {
    constructor() {
        this.recognition = null;
        this.isListening = false;
        this.btn = document.getElementById('btn-voice');
        this.feedback = document.getElementById('voice-feedback');
        this.voiceControls = document.getElementById('voice-controls');

        this.initSpeechRecognition();
    }

    initSpeechRecognition() {
        const SpeechRec = window.SpeechRecognition || window.webkitSpeechRecognition;

        if (SpeechRec) {
            this.recognition = new SpeechRec();
            this.recognition.continuous = true;
            this.recognition.interimResults = false;
            this.recognition.lang = 'es-ES';

            this.recognition.onstart = () => {
                this.isListening = true;
                if (this.voiceControls) this.voiceControls.classList.add('recording');
                if (this.btn) this.btn.style.color = '#10B981';
                this.showFeedback('🎙️ Escuchando comando de modelado...');
            };

            this.recognition.onresult = (event) => {
                const results = event.results;
                const lastResult = results[results.length - 1];
                if (lastResult && lastResult[0]) {
                    const transcript = lastResult[0].transcript.trim();
                    this.showFeedback(`🗣️ "${transcript}"`);
                    this.processCommand(transcript);
                }
            };

            this.recognition.onerror = (event) => {
                console.warn('[Voice Assistant] Error:', event.error);
                if (event.error === 'not-allowed') {
                    this.showFeedback('⚠️ Permiso de micrófono denegado en el navegador');
                } else if (event.error !== 'no-speech') {
                    this.showFeedback('Error en reconocimiento: ' + event.error);
                }
            };

            this.recognition.onend = () => {
                if (this.isListening) {
                    try {
                        this.recognition.start();
                    } catch(e) {}
                } else {
                    if (this.voiceControls) this.voiceControls.classList.remove('recording');
                    if (this.btn) this.btn.style.color = '';
                    this.showFeedback('Di: "crear clase Cliente", "relacionar Cliente con Compra"...');
                }
            };

            if (this.btn) {
                this.btn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    this.toggle();
                });
            }
            if (this.voiceControls) {
                this.voiceControls.addEventListener('click', () => this.toggle());
            }
        } else {
            console.warn('Web Speech API no soportada en este navegador.');
            if (this.feedback) {
                this.feedback.textContent = 'Micrófono no soportado en este navegador';
            }
        }
    }

    toggle() {
        if (!this.recognition) return;
        if (this.isListening) {
            this.isListening = false;
            try { this.recognition.stop(); } catch(e) {}
        } else {
            try { this.recognition.start(); } catch(e) {}
        }
    }

    speakResponse(text) {
        if ('speechSynthesis' in window) {
            window.speechSynthesis.cancel();
            const utterance = new SpeechSynthesisUtterance(text);
            utterance.lang = 'es-ES';
            utterance.rate = 1.05;
            window.speechSynthesis.speak(utterance);
        }
    }

    showFeedback(msg, isSuccess = false) {
        if (!this.feedback) return;
        this.feedback.innerHTML = msg;
        if (isSuccess) {
            this.feedback.style.color = '#10B981';
            this.feedback.style.fontWeight = '600';
            setTimeout(() => {
                if (this.feedback) {
                    this.feedback.style.color = '';
                    this.feedback.style.fontWeight = '';
                }
            }, 4500);
        }
    }

    processCommand(transcript) {
        const command = VoiceCommandParser.parse(transcript);

        if (!command) {
            this.showFeedback(`❓ Comando no reconocido: "${transcript}"`);
            return;
        }

        if (!window.CaseCollab) {
            this.showFeedback('⚠️ Lienzo colaborativo no inicializado');
            return;
        }

        switch (command.type) {
            case 'CREATE_CLASS': {
                const attrs = command.attributes && command.attributes.length > 0 ? command.attributes : [{ nombre: 'id', tipoDato: 'Long', esPk: true }];
                const nueva = window.CaseCollab.crearClaseConAtributos(command.className, attrs, command.stereotype);
                if (nueva) {
                    const msg = `✓ Clase '${command.className}' creada con ${attrs.length} atributos`;
                    this.showFeedback(msg, true);
                    this.speakResponse(`Clase ${command.className} creada exitosamente`);
                }
                break;
            }

            case 'CREATE_CLASSES_BATCH': {
                command.classNames.forEach((name, idx) => {
                    setTimeout(() => {
                        window.CaseCollab.crearClaseConAtributos(name, [{ nombre: 'id', tipoDato: 'Long', esPk: true }]);
                    }, idx * 250);
                });
                const msg = `✓ Creadas ${command.classNames.length} clases: ${command.classNames.join(', ')}`;
                this.showFeedback(msg, true);
                this.speakResponse(`Se crearon las clases ${command.classNames.join(', ')}`);
                break;
            }

            case 'ADD_ATTRIBUTE': {
                const ok = window.CaseCollab.agregarAtributoAClase(command.className, command.attrName, command.attrType);
                if (ok) {
                    const msg = `✓ Atributo '${command.attrName}: ${command.attrType}' añadido a ${command.className}`;
                    this.showFeedback(msg, true);
                    this.speakResponse(`Atributo ${command.attrName} añadido a ${command.className}`);
                } else {
                    this.showFeedback(`⚠️ No se encontró la clase '${command.className}'`);
                    this.speakResponse(`No encontré la clase ${command.className}`);
                }
                break;
            }

            case 'DELETE_ATTRIBUTE': {
                const ok = window.CaseCollab.eliminarAtributoDeClase(command.className, command.attrName);
                if (ok) {
                    const msg = `✓ Atributo '${command.attrName}' eliminado de ${command.className}`;
                    this.showFeedback(msg, true);
                    this.speakResponse(`Atributo ${command.attrName} eliminado de ${command.className}`);
                } else {
                    this.showFeedback(`⚠️ No se encontró el atributo '${command.attrName}' en '${command.className}'`);
                    this.speakResponse(`No se encontró el atributo ${command.attrName} en ${command.className}`);
                }
                break;
            }

            case 'CONNECT_CLASSES': {
                const ok = window.CaseCollab.crearRelacionEntreClases(
                    command.sourceClass,
                    command.targetClass,
                    command.relType || 'ASOCIACION',
                    command.cardOrig || '1',
                    command.cardDest || '1'
                );
                if (ok) {
                    const msg = `✓ Relación (${command.relType || 'ASOCIACION'}) creada entre ${command.sourceClass} y ${command.targetClass}`;
                    this.showFeedback(msg, true);
                    this.speakResponse(`Relacionadas las clases ${command.sourceClass} y ${command.targetClass}`);
                } else {
                    this.showFeedback(`⚠️ Verifica que existan las clases '${command.sourceClass}' y '${command.targetClass}'`);
                    this.speakResponse(`Verifica que existan las clases ${command.sourceClass} y ${command.targetClass}`);
                }
                break;
            }

            case 'DELETE_RELATION': {
                const ok = window.CaseCollab.eliminarRelacionEntreClases(command.sourceClass, command.targetClass);
                if (ok) {
                    const msg = `✓ Relación entre ${command.sourceClass} y ${command.targetClass} eliminada`;
                    this.showFeedback(msg, true);
                    this.speakResponse(`Relación entre ${command.sourceClass} y ${command.targetClass} eliminada`);
                } else {
                    this.showFeedback(`⚠️ No se encontró relación entre '${command.sourceClass}' y '${command.targetClass}'`);
                    this.speakResponse(`No se encontró relación entre ${command.sourceClass} y ${command.targetClass}`);
                }
                break;
            }

            case 'DELETE_CLASS': {
                const ok = window.CaseCollab.eliminarClasePorNombre(command.className, false);
                if (ok) {
                    this.showFeedback(`✓ Clase '${command.className}' eliminada`, true);
                    this.speakResponse(`Clase ${command.className} eliminada`);
                } else {
                    this.showFeedback(`⚠️ No se encontró la clase '${command.className}'`);
                    this.speakResponse(`No se encontró la clase ${command.className}`);
                }
                break;
            }

            case 'DELETE_CLASSES_BATCH': {
                let count = 0;
                command.classNames.forEach(name => {
                    const ok = window.CaseCollab.eliminarClasePorNombre(name, false);
                    if (ok) count++;
                });
                this.showFeedback(`✓ Se eliminaron ${count} clases: ${command.classNames.join(', ')}`, true);
                this.speakResponse(`Se eliminaron ${count} clases`);
                break;
            }

            case 'IMPORT_IMAGE': {
                this.showFeedback('📷 Abriendo importador de fotos y diagramas...', true);
                this.speakResponse('Abriendo importador de fotos y diagramas');
                const modal = document.getElementById('modal-import-image');
                if (modal) {
                    modal.style.display = 'flex';
                }
                if (typeof window.openImageImportModal === 'function') {
                    window.openImageImportModal();
                }
                break;
            }

            case 'GENERATE_BACKEND': {
                this.showFeedback('⚡ Generando arquitectura Backend Spring Boot...', true);
                this.speakResponse('Generando arquitectura backend completa con las entidades y relaciones del diagrama');
                
                const btnPreview = document.getElementById('btn-preview-code');
                if (btnPreview) {
                    btnPreview.click();
                }
                break;
            }

            case 'SAVE_DIAGRAM': {
                window.CaseCollab.guardarAvance(true);
                this.showFeedback('✓ Avance del diagrama guardado en base de datos', true);
                this.speakResponse('Diagrama guardado con éxito');
                break;
            }

            case 'SELECT_ALL': {
                window.CaseCollab.seleccionarTodas();
                this.showFeedback('✓ Todas las clases seleccionadas', true);
                break;
            }

            case 'DESELECT_ALL': {
                window.CaseCollab.deseleccionarTodas();
                this.showFeedback('✓ Selección limpiada', true);
                break;
            }
        }
    }
}

// Inicializar Asistente de Voz al cargar
window.addEventListener('DOMContentLoaded', () => {
    window.voiceAssistant = new VoiceAssistant();
});
