package com.payanam.rail.repository;

import com.payanam.rail.domain.audit.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Audit trail with a simple filter query (all filters optional). */
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {

    @Query("""
            select a from AuditLogEntry a
            where (:action is null or a.action = :action)
              and (:role is null or a.role = :role)
              and (:q is null or lower(a.actor) like lower(concat('%', :q, '%'))
                              or lower(a.details) like lower(concat('%', :q, '%')))
            order by a.timestamp desc, a.id desc
            """)
    Page<AuditLogEntry> search(@Param("action") String action, @Param("role") String role, @Param("q") String q,
                               Pageable pageable);

    @Query("select distinct a.action from AuditLogEntry a order by a.action")
    List<String> findDistinctActions();
}
