package com.student.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.student.entity.AuditLog;
import com.student.entity.Student;
import com.student.repository.AuditLogRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

// Writes audit entries. Who did it and from which IP is picked up from the
// current web request automatically, so callers only say WHAT happened.
//
// An audit problem must never break the real action (saving a student,
// logging in ...), so every failure here is caught and written to the normal
// log file instead of being thrown.
@Service
public class AuditService {

	private static final Logger log = LoggerFactory.getLogger(AuditService.class);

	// ---- action names (also used by the filter drop-down on the Audit Log page) ----
	public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
	public static final String LOGIN_FAILED = "LOGIN_FAILED";
	public static final String LOGOUT = "LOGOUT";
	public static final String STUDENT_CREATED = "STUDENT_CREATED";
	public static final String STUDENT_REGISTERED = "STUDENT_REGISTERED";
	public static final String STUDENT_UPDATED = "STUDENT_UPDATED";
	public static final String STUDENT_DELETED = "STUDENT_DELETED";
	public static final String STUDENT_VIEWED = "STUDENT_VIEWED";
	public static final String STUDENTS_EXPORTED = "STUDENTS_EXPORTED";
	public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
	public static final String STUDENT_APPROVED = "STUDENT_APPROVED";
	public static final String STUDENT_REJECTED = "STUDENT_REJECTED";

	public static final List<String> ALL_ACTIONS = List.of(LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, STUDENT_CREATED,
			STUDENT_REGISTERED, STUDENT_UPDATED, STUDENT_DELETED, STUDENT_VIEWED, STUDENTS_EXPORTED,
			PASSWORD_CHANGED, STUDENT_APPROVED, STUDENT_REJECTED);

	@Autowired
	private AuditLogRepository auditLogRepository;

	// Records an action done by whoever is logged in on the current request
	// ("anonymous" if nobody is).
	public void record(String action, String targetType, Object targetId, String details) {
		try {
			HttpServletRequest request = currentRequest();
			save(resolveActor(request), action, targetType, targetId, details, request);
		} catch (Exception ex) {
			log.error("Could not write audit log entry for action {}", action, ex);
		}
	}

	// Records an action with an explicit actor name (used for failed logins,
	// where nobody is logged in yet).
	public void recordAs(String actor, String action, String targetType, Object targetId, String details) {
		try {
			save(actor, action, targetType, targetId, details, currentRequest());
		} catch (Exception ex) {
			log.error("Could not write audit log entry for action {}", action, ex);
		}
	}

	private void save(String actor, String action, String targetType, Object targetId, String details,
			HttpServletRequest request) {
		AuditLog entry = new AuditLog(
				LocalDateTime.now(),
				clip(oneLine(actor), 100),
				action,
				targetType,
				targetId == null ? null : clip(String.valueOf(targetId), 50),
				clip(oneLine(details), 2000),
				clientIp(request));
		auditLogRepository.save(entry);
	}

	// Builds a readable "field: 'old' -> 'new'" list of what an edit changed.
	// Call this BEFORE saving the update, while 'before' still has the old values.
	public String describeChanges(Student before, Student after) {
		List<String> changes = new ArrayList<>();
		diff(changes, "name", before.getName(), after.getName());
		diff(changes, "age", before.getAge(), after.getAge());
		diff(changes, "course", before.getCourse(), after.getCourse());
		diff(changes, "cgpa", before.getCgpa(), after.getCgpa());
		diff(changes, "passoutYear", before.getPassoutYear(), after.getPassoutYear());
		diff(changes, "email", before.getEmail(), after.getEmail());
		diff(changes, "phone", before.getPhone(), after.getPhone());
		diff(changes, "domain", before.getDomain(), after.getDomain());
		diff(changes, "joiningDate", before.getJoiningDate(), after.getJoiningDate());
		diff(changes, "completionDate", before.getCompletionDate(), after.getCompletionDate());
		diff(changes, "collegeName", before.getCollegeName(), after.getCollegeName());
		diffMoney(changes, "totalFees", before.getTotalFees(), after.getTotalFees());
		diffMoney(changes, "feesPaid", before.getFeesPaid(), after.getFeesPaid());
		if (!Objects.equals(blankToNull(before.getPhotoUrl()), blankToNull(after.getPhotoUrl()))) {
			changes.add("photo: replaced");
		}
		return changes.isEmpty() ? "No fields changed" : String.join("; ", changes);
	}

	// ------------------------------------------------------------------ helpers

	private void diff(List<String> changes, String label, Object oldValue, Object newValue) {
		String o = blankToNull(oldValue == null ? null : oldValue.toString());
		String n = blankToNull(newValue == null ? null : newValue.toString());
		if (!Objects.equals(o, n)) {
			changes.add(label + ": '" + (o == null ? "" : o) + "' -> '" + (n == null ? "" : n) + "'");
		}
	}

	// 1000 and 1000.00 are the same amount, so compare numerically
	private void diffMoney(List<String> changes, String label, BigDecimal oldValue, BigDecimal newValue) {
		boolean same = (oldValue == null && newValue == null)
				|| (oldValue != null && newValue != null && oldValue.compareTo(newValue) == 0);
		if (!same) {
			changes.add(label + ": '" + (oldValue == null ? "" : oldValue.toPlainString()) + "' -> '"
					+ (newValue == null ? "" : newValue.toPlainString()) + "'");
		}
	}

	private HttpServletRequest currentRequest() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
			return attrs.getRequest();
		}
		return null;
	}

	private String resolveActor(HttpServletRequest request) {
		if (request != null) {
			HttpSession session = request.getSession(false);
			if (session != null && session.getAttribute("adminUsername") != null) {
				return String.valueOf(session.getAttribute("adminUsername"));
			}
		}
		return "anonymous";
	}

	// Best-effort: behind the Tailscale/Cloudflare proxy the real visitor address
	// arrives in X-Forwarded-For. If the proxy doesn't send it, this falls back to
	// the direct connection address (which would then be 127.0.0.1).
	private String clientIp(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
	 return clip(com.student.config.ClientIp.of(request), 64);
	}

	private String blankToNull(String s) {
		return (s == null || s.isBlank()) ? null : s.trim();
	}

	// Keeps each entry on a single line so nobody can fake extra log lines
	private String oneLine(String s) {
		return s == null ? null : s.replaceAll("[\\r\\n\\t]+", " ");
	}

	private String clip(String s, int max) {
		if (s == null || s.length() <= max) {
			return s;
		}
		return s.substring(0, max - 3) + "...";
	}
}
