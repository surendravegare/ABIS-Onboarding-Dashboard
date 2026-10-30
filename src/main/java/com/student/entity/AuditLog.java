package com.student.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

// One row = one thing that happened (a login, an edit, a delete ...).
// Rows are only ever inserted - there are deliberately no setters and no
// edit/delete screens, so the history can't be changed from inside the app.
@Entity
@Table(name = "audit_logs", indexes = {
		@Index(name = "idx_audit_created", columnList = "created_at"),
		@Index(name = "idx_audit_action", columnList = "action"),
		@Index(name = "idx_audit_actor", columnList = "actor")
})
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	// Admin username, or "anonymous" for people who are not logged in
	@Column(nullable = false, length = 100)
	private String actor;

	// e.g. LOGIN_SUCCESS, STUDENT_UPDATED (see AuditService constants)
	@Column(nullable = false, length = 50)
	private String action;

	@Column(name = "target_type", length = 50)
	private String targetType;

	@Column(name = "target_id", length = 50)
	private String targetId;

	@Column(length = 2000)
	private String details;

	@Column(name = "ip_address", length = 64)
	private String ipAddress;

	protected AuditLog() {
		// required by JPA
	}

	public AuditLog(LocalDateTime createdAt, String actor, String action, String targetType, String targetId,
			String details, String ipAddress) {
		this.createdAt = createdAt;
		this.actor = actor;
		this.action = action;
		this.targetType = targetType;
		this.targetId = targetId;
		this.details = details;
		this.ipAddress = ipAddress;
	}

	public Long getId() {
		return id;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public String getActor() {
		return actor;
	}

	public String getAction() {
		return action;
	}

	public String getTargetType() {
		return targetType;
	}

	public String getTargetId() {
		return targetId;
	}

	public String getDetails() {
		return details;
	}

	public String getIpAddress() {
		return ipAddress;
	}
}
