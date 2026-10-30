package com.student.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.student.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	// Empty string = "don't filter on this". (Empty strings are used instead
	// of nulls so the query behaves the same on every database.)
	@Query("SELECT a FROM AuditLog a WHERE "
			+ "(:action = '' OR a.action = :action) AND "
			+ "(:q = '' OR LOWER(a.actor) LIKE LOWER(CONCAT('%', :q, '%')) "
			+ "OR LOWER(a.details) LIKE LOWER(CONCAT('%', :q, '%')) "
			+ "OR LOWER(a.targetId) LIKE LOWER(CONCAT('%', :q, '%')))")
	Page<AuditLog> search(@Param("action") String action, @Param("q") String q, Pageable pageable);
}
