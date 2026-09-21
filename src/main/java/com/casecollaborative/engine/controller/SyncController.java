package com.casecollaborative.engine.controller;

import com.casecollaborative.engine.dto.sync.BatchSyncRequest;
import com.casecollaborative.engine.dto.sync.BatchSyncResponse;
import com.casecollaborative.engine.service.SyncConsolidationService;
import com.casecollaborative.engine.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final SyncConsolidationService syncConsolidationService;

    public SyncController(SyncConsolidationService syncConsolidationService) {
        this.syncConsolidationService = syncConsolidationService;
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyAuthority('ROLE_OPERADOR_MOVIL', 'ROLE_ANFITRION', 'ROLE_COLABORADOR')")
    public ResponseEntity<BatchSyncResponse> syncBatch(
            @RequestBody BatchSyncRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        BatchSyncResponse response = syncConsolidationService.procesarLote(
                request, 
                userDetails.getId(), 
                userDetails.getUsername()
        );
        return ResponseEntity.ok(response);
    }
}
