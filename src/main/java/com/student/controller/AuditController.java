package com.student.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.student.entity.AuditLog;
import com.student.repository.AuditLogRepository;
import com.student.service.AuditService;

// Read-only page listing the audit trail. It is NOT in the login-free list in
// WebConfig, so only logged-in admins can open it. There is intentionally no
// way to edit or delete entries from here.
@Controller
public class AuditController {

	private static final int PAGE_SIZE = 25;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@GetMapping("/audit")
	public String viewAuditLog(@RequestParam(defaultValue = "") String action,
			@RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "0") int page,
			Model model) {

		String actionFilter = action.trim();
		String textFilter = q.trim();

		// Newest first
		PageRequest pageRequest = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
				Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

		Page<AuditLog> logs = auditLogRepository.search(actionFilter, textFilter, pageRequest);

		model.addAttribute("logs", logs);
		model.addAttribute("actions", AuditService.ALL_ACTIONS);
		model.addAttribute("selectedAction", actionFilter);
		model.addAttribute("q", textFilter);
		return "audit-log";
	}
}
