// ==============================================
// APP STATE & GLOBALS
// ==============================================
const AppState = {
    jwtToken: localStorage.getItem('jwt_token') || null,
    user: {
        id: localStorage.getItem('usuario_id') || null,
        nombre: localStorage.getItem('usuario_nombre') || null,
        rol: localStorage.getItem('usuario_rol') || null
    },
    activeProject: null,
    activeSession: null
};

// Expose globals for canvas-collab.js, voice-assistant.js, etc.
window.jwtToken = AppState.jwtToken;
window.sessionToken = null;
window.proyectoId = null;
window.usuarioAutenticadoId = AppState.user.id;

function getAuthHeader() {
    const rawToken = localStorage.getItem('jwt_token');
    if (!rawToken) return {};
    const cleanToken = rawToken.startsWith('Bearer ') ? rawToken : `Bearer ${rawToken}`;
    return { 'Authorization': cleanToken };
}

// ==============================================
// DOM ELEMENTS
// ==============================================
const views = {
    auth: document.getElementById('view-auth'),
    dashboard: document.getElementById('view-dashboard'),
    workspace: document.getElementById('view-workspace')
};

// ==============================================
// ROUTING & NAVIGATION
// ==============================================
function navigateTo(viewId) {
    Object.values(views).forEach(v => v.classList.add('hidden'));
    views[viewId].classList.remove('hidden');

    if (viewId === 'dashboard') {
        renderDashboard();
        cargarProyectosUsuario();
    }
}

// Auto-login check
document.addEventListener('DOMContentLoaded', () => {
    if (AppState.jwtToken) {
        navigateTo('dashboard');
    } else {
        navigateTo('auth');
    }
});

// ==============================================
// VIEW 1: AUTHENTICATION
// ==============================================
const btnLogin = document.getElementById('btn-login');
const btnRegister = document.getElementById('btn-register');

// Tab Switching
document.querySelectorAll('#auth-tabs .tab').forEach(tab => {
    tab.addEventListener('click', (e) => {
        document.querySelectorAll('#auth-tabs .tab').forEach(t => t.classList.remove('active'));
        e.target.classList.add('active');
        
        document.getElementById('login-form').classList.add('hidden');
        document.getElementById('register-form').classList.add('hidden');
        
        const targetId = e.target.getAttribute('data-target');
        document.getElementById(targetId).classList.remove('hidden');
    });
});

function mostrarVistaDashboard() {
    navigateTo('dashboard');
}

async function cargarProyectosUsuario() {
    const container = document.getElementById('dashboard-proyectos-container');
    if (!container) return;
    
    container.innerHTML = '<div style="color: var(--text-secondary); font-size: 14px;">Cargando proyectos...</div>';
    
    try {
        const response = await fetch('/api/v1/proyectos', {
            method: 'GET',
            headers: getAuthHeader()
        });
        
        if (response.ok) {
            const proyectos = await response.json();
            if (proyectos.length === 0) {
                container.innerHTML = '<div style="color: var(--text-secondary); font-size: 14px;">No tienes proyectos creados aún. Inicia uno nuevo arriba.</div>';
                return;
            }
            
            container.innerHTML = '';
            proyectos.forEach(p => {
                const div = document.createElement('div');
                div.className = 'card flex items-center justify-between hover:bg-[var(--bg-canvas)]';
                div.style.padding = '14px 20px';
                div.style.transition = 'all 0.2s ease';
                div.style.cursor = 'pointer';
                div.innerHTML = `
                    <div class="flex items-center gap-4 project-info-area" style="flex: 1;">
                        <div style="color: var(--accent-primary); width: 36px; height: 36px; border-radius: 8px; background: rgba(59, 130, 246, 0.12); display: flex; align-items: center; justify-content: center;">
                            <span class="material-icons" style="font-size: 20px;">account_tree</span>
                        </div>
                        <div>
                            <div style="font-weight: 600; font-size: 15px; color: var(--text-primary);" class="project-name-label">${p.nombre}</div>
                            <div style="font-size: 12px; color: var(--text-secondary); margin-top: 2px;">ID: ${p.id} • ${p.fechaCreacion ? new Date(p.fechaCreacion).toLocaleDateString() : 'Reciente'}</div>
                        </div>
                    </div>
                    <div class="flex items-center gap-2">
                        <button class="btn btn-ghost btn-edit-pname" style="padding: 6px; color: #60A5FA;" title="Cambiar nombre del proyecto">
                            <span class="material-icons" style="font-size: 18px;">edit</span>
                        </button>
                        <button class="btn btn-ghost btn-delete-p" style="padding: 6px; color: var(--status-danger);" title="Eliminar proyecto">
                            <span class="material-icons" style="font-size: 18px;">delete</span>
                        </button>
                        <button class="btn btn-primary btn-open-p" style="padding: 6px 14px; font-size: 12px;" title="Abrir Workspace">
                            Abrir <span class="material-icons" style="font-size: 14px;">arrow_forward</span>
                        </button>
                    </div>
                `;
                
                // Función para abrir el workspace
                const abrirProyecto = async () => {
                    localStorage.setItem('proyecto_id', p.id);
                    try {
                        const respSesion = await fetch('/api/v1/sesiones/crear', {
                            method: 'POST',
                            headers: {
                                ...getAuthHeader(),
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify({ proyectoId: p.id })
                        });
                        
                        if (respSesion.ok) {
                            const data = await respSesion.json();
                            const sessionToken = data.sessionToken || data.token;
                            localStorage.setItem('session_token', sessionToken);
                            mostrarVistaWorkspace(sessionToken, p.id, p.nombre);
                        } else {
                            alert('Error al abrir la sesión colaborativa del proyecto');
                        }
                    } catch (err) {
                        console.error('Error al abrir sala:', err);
                        alert('Error de conexión al abrir sala.');
                    }
                };

                // Clic en la fila o botón Abrir
                div.querySelector('.project-info-area').addEventListener('click', abrirProyecto);
                div.querySelector('.btn-open-p').addEventListener('click', (e) => {
                    e.stopPropagation();
                    abrirProyecto();
                });

                // Cambiar Nombre
                div.querySelector('.btn-edit-pname').addEventListener('click', async (e) => {
                    e.stopPropagation();
                    const nuevoNombre = prompt(`Nuevo nombre para el proyecto "${p.nombre}":`, p.nombre);
                    if (nuevoNombre && nuevoNombre.trim() && nuevoNombre.trim() !== p.nombre) {
                        try {
                            const res = await fetch(`/api/v1/proyectos/${p.id}`, {
                                method: 'PUT',
                                headers: {
                                    ...getAuthHeader(),
                                    'Content-Type': 'application/json'
                                },
                                body: JSON.stringify({ nombre: nuevoNombre.trim() })
                            });
                            if (res.ok) {
                                p.nombre = nuevoNombre.trim();
                                div.querySelector('.project-name-label').textContent = p.nombre;
                            } else {
                                alert('No se pudo actualizar el nombre.');
                            }
                        } catch(err) {
                            alert('Error al actualizar nombre.');
                        }
                    }
                });

                // Eliminar Proyecto
                div.querySelector('.btn-delete-p').addEventListener('click', async (e) => {
                    e.stopPropagation();
                    if (confirm(`¿Estás seguro de eliminar permanentemente el proyecto "${p.nombre}" y todo su diagrama?`)) {
                        try {
                            const res = await fetch(`/api/v1/proyectos/${p.id}`, {
                                method: 'DELETE',
                                headers: getAuthHeader()
                            });
                            if (res.ok) {
                                div.remove();
                                if (container.children.length === 0) {
                                    container.innerHTML = '<div style="color: var(--text-secondary); font-size: 14px;">No tienes proyectos creados aún. Inicia uno nuevo arriba.</div>';
                                }
                            } else {
                                alert('Error al eliminar proyecto.');
                            }
                        } catch(err) {
                            alert('Error de conexión al eliminar.');
                        }
                    }
                });
                
                container.appendChild(div);
            });
        }
    } catch (e) {
        console.error(e);
        container.innerHTML = '<div style="color: var(--status-danger); font-size: 14px;">Error al cargar proyectos</div>';
    }
}

// Login
btnLogin.addEventListener('click', async () => {
    const correo = document.getElementById('login-correo').value.trim();
    const password = document.getElementById('login-password').value;

    try {
        const response = await fetch('/api/v1/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ correo, password })
        });

        if (response.ok) {
            const data = await response.json();
            saveSession(data.token, data.id, data.nombre || data.correo, data.rol);
            mostrarVistaDashboard();
        } else {
            alert('Credenciales inválidas. Verifica tu correo y contraseña.');
        }
    } catch (e) {
        console.error('Error logging in:', e);
        alert('Error al conectar con el servidor.');
    }
});

// Register
btnRegister.addEventListener('click', async () => {
    const nombre = document.getElementById('reg-nombre').value.trim();
    const apellido = document.getElementById('reg-apellido').value.trim();
    const correo = document.getElementById('reg-correo').value.trim();
    const password = document.getElementById('reg-password').value;

    try {
        const response = await fetch('/api/v1/auth/registro', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nombre, apellido, correo, password })
        });

        if (response.ok) {
            const data = await response.json();
            saveSession(data.token, data.id || null, data.nombre || 'Usuario', data.rol || 'ANFITRION');
            alert('Cuenta creada exitosamente. Redirigiendo al Workspace...');
            mostrarVistaDashboard();
        } else {
            const err = await response.text();
            alert('Error al registrar cuenta: ' + err);
        }
    } catch (e) {
        console.error('Error registering:', e);
        alert('Error al conectar con el servidor.');
    }
});

function saveSession(token, id, nombre, rol) {
    localStorage.setItem('jwt_token', token);
    localStorage.setItem('usuario_id', id);
    localStorage.setItem('usuario_nombre', nombre);
    localStorage.setItem('usuario_rol', rol);

    AppState.jwtToken = token;
    AppState.user = { id, nombre, rol };
    
    window.jwtToken = token;
    window.usuarioAutenticadoId = id;
}

// ==============================================
// VIEW 2: DASHBOARD
// ==============================================
const btnLogout = document.getElementById('btn-logout');
const btnCreateSession = document.getElementById('btn-crear-sala');
const btnJoinSession = document.getElementById('btnConnect');

function renderDashboard() {
    const userNameEl = document.getElementById('dash-user-name');
    const userRoleEl = document.getElementById('dash-user-role');
    const userAvatarEl = document.getElementById('dash-user-avatar');

    if (userNameEl && AppState.user.nombre) {
        userNameEl.textContent = AppState.user.nombre;
        const initials = AppState.user.nombre.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
        if (userAvatarEl) userAvatarEl.textContent = initials;
    }
    if (userRoleEl && AppState.user.rol) {
        userRoleEl.textContent = AppState.user.rol;
    }
}

btnLogout.addEventListener('click', () => {
    localStorage.clear();
    AppState.jwtToken = null;
    AppState.user = { id: null, nombre: null, rol: null };
    window.jwtToken = null;
    
    // Todo: Disconnect WebSocket if active
    if (window.stompClient && window.stompClient.connected) {
        window.stompClient.deactivate();
    }
    
    navigateTo('auth');
});

btnCreateSession.addEventListener('click', manejarCreacionSala);

async function manejarCreacionSala() {
    const token = localStorage.getItem('jwt_token');
    if (!token) {
        alert('Sesión expirada. Por favor vuelve a iniciar sesión.');
        navigateTo('auth');
        return;
    }

    const inputNombre = document.getElementById('input-nombre-proyecto');
    const nombreProyecto = (inputNombre && inputNombre.value.trim()) ? inputNombre.value.trim() : 'Nuevo Diagrama UML';

    try {
        // PASO 1: Crear el proyecto en PostgreSQL (o asegurar uno existente)
        const respProyecto = await fetch('/api/v1/proyectos', {
            method: 'POST',
            headers: {
                ...getAuthHeader(),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                nombre: nombreProyecto,
                descripcion: 'Proyecto generado desde Workspace'
            })
        });

        if (!respProyecto.ok) {
            const errP = await respProyecto.text();
            throw new Error(`Error al crear proyecto: ${respProyecto.status} - ${errP}`);
        }

        const proyectoData = await respProyecto.json();
        const nuevoProyectoId = proyectoData.id;
        localStorage.setItem('proyecto_id', nuevoProyectoId);

        // PASO 2: Crear la sesión colaborativa enlazada al proyecto real
        const respSesion = await fetch('/api/v1/sesiones/crear', {
            method: 'POST',
            headers: {
                ...getAuthHeader(),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                proyectoId: nuevoProyectoId
            })
        });

        if (!respSesion.ok) {
            const errS = await respSesion.text();
            throw new Error(`Error al crear sesión: ${respSesion.status} - ${errS}`);
        }

        const sesionData = await respSesion.json();
        const sessionToken = sesionData.sessionToken;
        localStorage.setItem('session_token', sessionToken);

        // PASO 3: Transicionar a la Vista de Lienzo (Workspace) y conectar WebSockets
        mostrarVistaWorkspace(sessionToken, nuevoProyectoId, nombreProyecto);

    } catch (error) {
        console.error('Fallo en la inicialización de sala:', error);
        alert(error.message);
    }
}

btnJoinSession.addEventListener('click', async () => {
    const token = document.getElementById('sessionToken').value.trim();
    if (!token) return alert('Por favor ingresa un Session Token (UUID) válido');
    
    try {
        const response = await fetch(`/api/v1/sesiones/${token}/unirse`, {
            method: 'POST',
            headers: getAuthHeader()
        });
        
        if (response.ok) {
            const snapshot = await response.json();
            window.sessionToken = token;
            const pId = snapshot.proyectoId || localStorage.getItem('proyecto_id') || 1;
            const pName = snapshot.nombre || "Proyecto Colaborativo";
            
            localStorage.setItem('session_token', token);
            localStorage.setItem('proyecto_id', pId);
            
            // Siempre que se ingresa mediante código UUID, adquiere el rol de COLABORADOR
            localStorage.setItem('usuario_rol', 'COLABORADOR');
            if (AppState.user) {
                AppState.user.rol = 'COLABORADOR';
            }
            const userRoleEl = document.getElementById('dash-user-role');
            if (userRoleEl) {
                userRoleEl.textContent = 'COLABORADOR';
                userRoleEl.className = 'tag tag-blue';
            }

            mostrarVistaWorkspace(token, pId, pName);

            // Cargar de inmediato las clases y relaciones recibidas del servidor
            if (window.CaseCollab && window.CaseCollab.cargarSnapshot) {
                window.CaseCollab.cargarSnapshot(snapshot);
            }
        } else {
            const err = await response.text();
            alert('No se pudo unir a la sesión. Verifica que el Session Token sea válido y esté activo.');
        }
    } catch (e) {
        console.error('Error al unirse a la sesión:', e);
        alert('Error de conexión al unirse a la sesión.');
    }
});

// ==============================================
// VIEW 3: WORKSPACE
// ==============================================
const btnBackDashboard = document.getElementById('btn-back-dashboard');
const btnImportXmi = document.getElementById('btn-import-xmi');
const btnExportXmi = document.getElementById('btn-export-xmi');
const btnPreviewCode = document.getElementById('btn-preview-code');
const btnExportZipFinal = document.getElementById('btn-export-zip-final');
const btnVoice = document.getElementById('btn-voice');
const lblSessionToken = document.getElementById('lbl-session-token');

function mostrarVistaWorkspace(sessionToken, proyectoId, nombreProyecto) {
    window.sessionToken = sessionToken;
    window.proyectoId = proyectoId;
    
    // Navegar y activar vista
    Object.values(views).forEach(v => v.classList.add('hidden'));
    views.workspace.classList.remove('hidden');

    // Actualiza el título del proyecto en la barra superior
    const lblProjectName = document.getElementById('workspace-project-name');
    if (lblProjectName && nombreProyecto) {
        lblProjectName.textContent = nombreProyecto;
    }
    if (lblSessionToken) {
        lblSessionToken.textContent = '#' + sessionToken.substring(0, 8) + '...';
        lblSessionToken.onclick = () => {
            navigator.clipboard.writeText(sessionToken);
            alert('Token copiado: ' + sessionToken);
        };
    }

    // Iniciar Canvas (ya no es un alert de fallback)
    if (window.CaseCollab) {
        window.CaseCollab.init(sessionToken, proyectoId, AppState.jwtToken);
    } else {
        console.error('El script del lienzo no está cargado correctamente');
    }
    
    // Redimensionar canvas
    const canvas = document.getElementById('lienzo');
    if (canvas) {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
    }
}

btnBackDashboard.addEventListener('click', async () => {
    if (window.CaseCollab && typeof window.CaseCollab.guardarAvance === 'function') {
        try {
            await window.CaseCollab.guardarAvance(false);
        } catch(e) {}
    }
    if (window.CaseCollab && typeof window.CaseCollab.disconnect === 'function') {
        window.CaseCollab.disconnect();
    }
    if (window.stompClient && window.stompClient.connected) {
        window.stompClient.deactivate();
    }
    navigateTo('dashboard');
    cargarProyectosUsuario();
});

async function descargarArchivoAutenticado(endpointUrl, nombreArchivoPorDefecto) {
    const token = localStorage.getItem('jwt_token');
    if (!token) {
        alert('Debes iniciar sesión para exportar.');
        return;
    }

    try {
        const response = await fetch(endpointUrl, {
            method: 'GET',
            headers: getAuthHeader()
        });

        if (!response.ok) {
            throw new Error(`Error en descarga: ${response.status} ${response.statusText}`);
        }

        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = nombreArchivoPorDefecto;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(downloadUrl);
    } catch (err) {
        console.error('Fallo al exportar:', err);
        alert('No se pudo descargar el archivo: ' + err.message);
    }
}

// Exportaciones
if (btnExportXmi) {
    btnExportXmi.addEventListener('click', () => {
        const pId = localStorage.getItem('proyecto_id') || window.proyectoId;
        if (!pId) {
            alert('Debes abrir o crear un proyecto primero');
            return;
        }
        descargarArchivoAutenticado(`/api/v1/proyectos/${pId}/exportar/xmi`, `diagrama_proyecto_${pId}.xmi`);
    });
}
if (btnPreviewCode) {
    btnPreviewCode.addEventListener('click', async () => {
        const pId = localStorage.getItem('proyecto_id') || window.proyectoId;
        if (!pId) {
            alert('Debes abrir o crear un proyecto primero');
            return;
        }
        
        try {
            document.getElementById('modal-preview-code').style.display = 'flex';
            document.getElementById('preview-code-content').textContent = 'Generando DDL...';
            
            const response = await fetch(`/api/v1/proyectos/${pId}/generar/ddl?tipo=SPRING_BOOT_POSTGRES`, {
                headers: getAuthHeader()
            });
            
            if (response.ok) {
                const code = await response.text();
                document.getElementById('preview-code-content').textContent = code;
            } else {
                document.getElementById('preview-code-content').textContent = 'Error al generar código: ' + response.status;
            }
        } catch (e) {
            document.getElementById('preview-code-content').textContent = 'Fallo de conexión al generar código.';
        }
    });
}
if (btnExportZipFinal) {
    btnExportZipFinal.addEventListener('click', () => {
        const pId = localStorage.getItem('proyecto_id') || window.proyectoId;
        if (pId) {
            descargarArchivoAutenticado(`/api/v1/proyectos/${pId}/generar/backend-zip?tipo=SPRING_BOOT_POSTGRES`, `backend_spring_boot_${pId}.zip`);
        }
        document.getElementById('modal-preview-code').style.display = 'none';
    });
}

document.getElementById('input-file-xmi')?.addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const proyectoId = localStorage.getItem('proyecto_id');
    if (!proyectoId) {
        alert('Debes abrir o crear un proyecto primero');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    const res = await fetch(`/api/v1/proyectos/${proyectoId}/importar/xmi`, {
        method: 'POST',
        headers: getAuthHeader(),
        body: formData
    });

    if (res.ok) {
        // En lugar de reload, usamos el motor del lienzo
        if (window.CaseCollab) {
            window.CaseCollab.recargarDiagrama(proyectoId);
            // Mostrar toast no bloqueante
            const toast = document.createElement('div');
            toast.textContent = "Modelo XMI importado y sincronizado en el lienzo";
            toast.style.cssText = "position:absolute; bottom: 20px; left: 50%; transform: translateX(-50%); background: var(--status-success); padding: 12px 24px; border-radius: 8px; z-index: 9999;";
            document.body.appendChild(toast);
            setTimeout(() => toast.remove(), 3000);
        } else {
            location.reload();
        }
    } else {
        alert('Error al importar archivo XMI.');
    }
});

// Botones para archivos .case
const btnExportCase = document.getElementById('btn-export-case');
if (btnExportCase) {
    btnExportCase.addEventListener('click', () => {
        const pId = localStorage.getItem('proyecto_id') || window.proyectoId;
        if (!pId) {
            alert('Debes abrir o crear un proyecto primero');
            return;
        }
        descargarArchivoAutenticado(`/api/v1/proyectos/${pId}/exportar/case`, `proyecto_${pId}.case`);
    });
}

document.getElementById('input-file-case')?.addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const proyectoId = localStorage.getItem('proyecto_id');
    if (!proyectoId) {
        alert('Debes abrir o crear un proyecto primero');
        return;
    }

    const formData = new FormData();
    // /importar/case usa @RequestBody String jsonContent, así que leemos el archivo como texto
    const reader = new FileReader();
    reader.onload = async function(evt) {
        const content = evt.target.result;
        const res = await fetch(`/api/v1/proyectos/${proyectoId}/importar/case`, {
            method: 'POST',
            headers: {
                ...getAuthHeader(),
                'Content-Type': 'application/json'
            },
            body: content
        });

        if (res.ok) {
            if (window.CaseCollab) {
                window.CaseCollab.recargarDiagrama(proyectoId);
            } else {
                location.reload();
            }
        } else {
            alert('Error al importar archivo .case.');
        }
    };
    reader.readAsText(file);
});

// ==========================================
// IMPORTADOR DE IMAGEN / FOTO UML CON IA (GEMINI MULTIMODAL)
// ==========================================
const btnOpenImportImage = document.getElementById('btn-open-import-image');
const modalImportImage = document.getElementById('modal-import-image');
const btnCloseImportImage = document.getElementById('btn-close-import-image');
const btnCancelImportImage = document.getElementById('btn-cancel-import-image');
const btnSubmitImportImage = document.getElementById('btn-submit-import-image');
const imageDropzone = document.getElementById('image-dropzone');
const inputDiagramImage = document.getElementById('input-diagram-image');
const imagePreviewContainer = document.getElementById('image-preview-container');
const imagePreview = document.getElementById('image-preview');
const imagePreviewInfo = document.getElementById('image-preview-info');
const btnRemoveSelectedImage = document.getElementById('btn-remove-selected-image');
const inputGeminiApiKey = document.getElementById('input-gemini-api-key');
const imageProcessStatus = document.getElementById('image-process-status');
const imageProcessMsg = document.getElementById('image-process-msg');

let selectedImageFile = null;

// Cargar API Key guardada de sesiones previas si existe
if (inputGeminiApiKey) {
    const savedKey = localStorage.getItem('gemini_api_key');
    if (savedKey) inputGeminiApiKey.value = savedKey;
    inputGeminiApiKey.addEventListener('input', () => {
        if (inputGeminiApiKey.value.trim()) {
            localStorage.setItem('gemini_api_key', inputGeminiApiKey.value.trim());
        }
    });
}

window.openImageImportModal = function() {
    const modal = document.getElementById('modal-import-image');
    if (modal) {
        modal.style.display = 'flex';
        resetImageImportState();
    } else {
        console.warn('Modal modal-import-image no encontrado en el DOM');
    }
};

window.closeImageImportModal = function() {
    const modal = document.getElementById('modal-import-image');
    if (modal) {
        modal.style.display = 'none';
        resetImageImportState();
    }
};

// Cerrar al hacer clic en el backdrop oscuro
const modalEl = document.getElementById('modal-import-image');
if (modalEl) {
    modalEl.addEventListener('click', (e) => {
        if (e.target === modalEl) {
            window.closeImageImportModal();
        }
    });
}

function resetImageImportState() {
    selectedImageFile = null;
    const inputDiagramImage = document.getElementById('input-diagram-image');
    const imagePreviewContainer = document.getElementById('image-preview-container');
    const imageDropzone = document.getElementById('image-dropzone');
    const imageProcessStatus = document.getElementById('image-process-status');
    const btnSubmit = document.getElementById('btn-submit-import-image');

    if (inputDiagramImage) inputDiagramImage.value = '';
    if (imagePreviewContainer) imagePreviewContainer.style.display = 'none';
    if (imageDropzone) imageDropzone.style.display = 'block';
    if (imageProcessStatus) imageProcessStatus.style.display = 'none';
    if (btnSubmit) {
        btnSubmit.disabled = true;
        btnSubmit.innerHTML = '<span class="material-icons" style="font-size: 16px;">auto_fix_high</span> Analizar y Reconstruir en Lienzo';
    }
}

function handleImageSelected(file) {
    if (!file) return;
    if (!file.type.startsWith('image/')) {
        alert('Por favor selecciona un archivo de imagen válido (PNG, JPG, JPEG, WEBP, BMP).');
        return;
    }

    selectedImageFile = file;
    const reader = new FileReader();
    reader.onload = (e) => {
        if (imagePreview) imagePreview.src = e.target.result;
        if (imagePreviewInfo) {
            const sizeKB = Math.round(file.size / 1024);
            imagePreviewInfo.textContent = `📷 ${file.name} (${sizeKB} KB - ${file.type})`;
        }
        if (imageDropzone) imageDropzone.style.display = 'none';
        if (imagePreviewContainer) imagePreviewContainer.style.display = 'block';
        if (btnSubmitImportImage) btnSubmitImportImage.disabled = false;
    };
    reader.readAsDataURL(file);
}

if (btnOpenImportImage) btnOpenImportImage.addEventListener('click', openImageImportModal);
if (btnCloseImportImage) btnCloseImportImage.addEventListener('click', closeImageImportModal);
if (btnCancelImportImage) btnCancelImportImage.addEventListener('click', closeImageImportModal);

if (imageDropzone && inputDiagramImage) {
    imageDropzone.addEventListener('click', () => inputDiagramImage.click());
    inputDiagramImage.addEventListener('change', (e) => {
        if (e.target.files && e.target.files[0]) {
            handleImageSelected(e.target.files[0]);
        }
    });

    // Drag and Drop
    imageDropzone.addEventListener('dragover', (e) => {
        e.preventDefault();
        imageDropzone.style.borderColor = '#A78BFA';
        imageDropzone.style.background = 'rgba(139, 92, 246, 0.15)';
    });

    imageDropzone.addEventListener('dragleave', (e) => {
        e.preventDefault();
        imageDropzone.style.borderColor = '#8B5CF6';
        imageDropzone.style.background = 'rgba(139, 92, 246, 0.05)';
    });

    imageDropzone.addEventListener('drop', (e) => {
        e.preventDefault();
        imageDropzone.style.borderColor = '#8B5CF6';
        imageDropzone.style.background = 'rgba(139, 92, 246, 0.05)';
        if (e.dataTransfer.files && e.dataTransfer.files[0]) {
            handleImageSelected(e.dataTransfer.files[0]);
        }
    });
}

if (btnRemoveSelectedImage) {
    btnRemoveSelectedImage.addEventListener('click', (e) => {
        e.stopPropagation();
        resetImageImportState();
    });
}

if (btnSubmitImportImage) {
    btnSubmitImportImage.addEventListener('click', async () => {
        if (!selectedImageFile) return;

        const proyectoId = localStorage.getItem('proyecto_id') || window.proyectoId;
        if (!proyectoId) {
            alert('Debes abrir o crear un proyecto primero');
            return;
        }

        const apiKey = inputGeminiApiKey ? inputGeminiApiKey.value.trim() : '';

        // UI Loading
        btnSubmitImportImage.disabled = true;
        btnSubmitImportImage.innerHTML = '<span class="material-icons" style="font-size: 16px;">hourglass_top</span> Procesando...';
        if (imageProcessStatus) imageProcessStatus.style.display = 'block';
        if (imageProcessMsg) imageProcessMsg.textContent = 'Analizando diagrama con visión multimodal y extrayendo topología...';

        try {
            const formData = new FormData();
            formData.append('file', selectedImageFile);
            if (apiKey) {
                formData.append('apiKey', apiKey);
            }

            const headers = { ...getAuthHeader() };
            if (apiKey) {
                headers['X-Gemini-Api-Key'] = apiKey;
            }

            const response = await fetch(`/api/v1/proyectos/${proyectoId}/importar/imagen`, {
                method: 'POST',
                headers: headers,
                body: formData
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || errorData.error || `Error en el servidor (HTTP ${response.status})`);
            }

            const result = await response.json();
            closeImageImportModal();

            // Sincronizar y recargar en lienzo
            if (window.CaseCollab) {
                window.CaseCollab.recargarDiagrama(proyectoId);
            }

            // Notificación visual de éxito
            const toast = document.createElement('div');
            toast.style.cssText = "position:fixed; bottom: 30px; left: 50%; transform: translateX(-50%); background: linear-gradient(135deg, #8B5CF6, #6366F1); color: white; padding: 14px 28px; border-radius: 10px; font-weight: 600; box-shadow: 0 10px 25px rgba(139, 92, 246, 0.5); z-index: 10000; display: flex; align-items: center; gap: 8px;";
            toast.innerHTML = `<span class="material-icons">auto_awesome</span> ✨ ${result.mensaje || 'Diagrama importado con éxito'}`;
            document.body.appendChild(toast);
            setTimeout(() => toast.remove(), 4500);

            if (window.voiceAssistant && typeof window.voiceAssistant.speakResponse === 'function') {
                window.voiceAssistant.speakResponse(`Diagrama importado exitosamente con ${result.clasesImportadas} clases`);
            }

        } catch (err) {
            console.error('Error importando imagen:', err);
            alert('Error al importar diagrama desde imagen: ' + err.message);
            if (btnSubmitImportImage) {
                btnSubmitImportImage.disabled = false;
                btnSubmitImportImage.innerHTML = '<span class="material-icons" style="font-size: 16px;">auto_fix_high</span> Reintentar';
            }
            if (imageProcessStatus) imageProcessStatus.style.display = 'none';
        }
    });
}

