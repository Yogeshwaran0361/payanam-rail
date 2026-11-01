package com.payanam.rail.controller;

import com.payanam.rail.dto.PageView;
import com.payanam.rail.dto.station.AuditEntryView;
import com.payanam.rail.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Audit log with filters and CSV export (Station Master). */
@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasAuthority('PERM_VIEW_AUDIT')")
@Tag(name = "audit", description = "Audit trail")
public class AuditController {
    private final AuditService audit;

    public AuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping
    @Operation(summary = "Filtered audit log, newest first")
    public PageView<AuditEntryView> search(@RequestParam(required = false) String action,
                                           @RequestParam(required = false) String role,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "25") int size) {
        return audit.search(action, role, q, page, size);
    }

    @GetMapping("/actions")
    @Operation(summary = "Distinct action names (for the filter)")
    public List<String> actions() {
        return audit.actions();
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "CSV export of the filtered log")
    public ResponseEntity<String> export(@RequestParam(required = false) String action,
                                         @RequestParam(required = false) String role,
                                         @RequestParam(required = false) String q) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payanam-audit-log.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(audit.exportCsv(action, role, q));
    }
}
