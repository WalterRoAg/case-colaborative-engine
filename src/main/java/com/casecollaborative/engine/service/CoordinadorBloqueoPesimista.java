package com.casecollaborative.engine.service;

import com.casecollaborative.engine.repository.ClaseRepository;
import com.casecollaborative.engine.repository.UsuarioRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@EnableAsync
public class CoordinadorBloqueoPesimista {

    private final ClaseRepository claseRepository;
    private final UsuarioRepository usuarioRepository;

    // Map of claseId -> LockEntry
    private final Map<Long, LockEntry> locks = new ConcurrentHashMap<>();
    
    // Map of claseId -> last updated client timestamp for LWW
    private final Map<Long, Long> lastClientTimestamps = new ConcurrentHashMap<>();

    public CoordinadorBloqueoPesimista(ClaseRepository claseRepository, UsuarioRepository usuarioRepository) {
        this.claseRepository = claseRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public synchronized boolean intentarAdquirirLock(Long claseId, Long usuarioId, String usuarioNombre) {
        LockEntry currentLock = locks.get(claseId);
        
        if (currentLock == null || currentLock.usuarioId().equals(usuarioId)) {
            locks.put(claseId, new LockEntry(usuarioId, usuarioNombre, System.currentTimeMillis()));
            persistirBloqueoAsync(claseId, usuarioId);
            return true;
        }
        return false;
    }

    public synchronized boolean liberarLock(Long claseId, Long usuarioId) {
        LockEntry currentLock = locks.get(claseId);
        if (currentLock != null && currentLock.usuarioId().equals(usuarioId)) {
            locks.remove(claseId);
            persistirDesbloqueoAsync(claseId);
            return true;
        }
        return false;
    }

    public synchronized void liberarTodosLocksDeUsuario(Long usuarioId) {
        Iterator<Map.Entry<Long, LockEntry>> iterator = locks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, LockEntry> entry = iterator.next();
            if (entry.getValue().usuarioId().equals(usuarioId)) {
                iterator.remove();
                persistirDesbloqueoAsync(entry.getKey());
            }
        }
    }

    public LockEntry getLock(Long claseId) {
        return locks.get(claseId);
    }
    
    public synchronized boolean validarLWW(Long claseId, Long clientTimestamp) {
        Long lastTimestamp = lastClientTimestamps.getOrDefault(claseId, 0L);
        if (clientTimestamp >= lastTimestamp) {
            lastClientTimestamps.put(claseId, clientTimestamp);
            return true;
        }
        return false;
    }

    @Async
    protected void persistirBloqueoAsync(Long claseId, Long usuarioId) {
        // En una app real, aquí se hace fetch y save. Para write-behind rápido, puede usarse custom query:
        // claseRepository.actualizarBloqueo(claseId, usuarioId, new Timestamp(System.currentTimeMillis()));
        claseRepository.findById(claseId).ifPresent(clase -> {
            clase.setBloqueadoPor(usuarioRepository.getReferenceById(usuarioId));
            clase.setBloqueadoEn(ZonedDateTime.now());
            claseRepository.save(clase);
        });
    }

    @Async
    protected void persistirDesbloqueoAsync(Long claseId) {
        claseRepository.findById(claseId).ifPresent(clase -> {
            clase.setBloqueadoPor(null);
            clase.setBloqueadoEn(null);
            claseRepository.save(clase);
        });
    }

    public record LockEntry(Long usuarioId, String usuarioNombre, Long timestampAdquisicion) {}
}
