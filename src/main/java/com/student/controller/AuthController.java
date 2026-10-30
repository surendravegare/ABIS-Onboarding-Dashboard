package com.student.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.student.config.ClientIp;
import com.student.service.AdminService;
import com.student.service.AuditService;
import com.student.service.RequestThrottle;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

	private static final int MIN_PASSWORD_LENGTH = 8;

	@Autowired
	private AdminService adminService;

	@Autowired
	private AuditService auditService;

	@Autowired
	private RequestThrottle throttle;

	// Login Page (shown before the dashboard/index page)
	@GetMapping("/login")
	public String loginPage(HttpSession session) {
		if (session.getAttribute("adminUsername") != null) {
			return "redirect:/";
		}
		return "login";
	}

	// Handle Login
	// Admin id/password are stored in the "admins" table (password as a BCrypt hash).
	@PostMapping("/login")
	public String doLogin(@RequestParam String username,
			@RequestParam String password,
			HttpServletRequest request,
			HttpSession session,
			Model model) {

		String ip = ClientIp.of(request);
		String userKey = username.trim() + "|" + ip;

		// Too many wrong passwords recently (for this username or from this address)
		if (throttle.isBlocked(RequestThrottle.LOGIN_USER, userKey) || throttle.isBlocked(RequestThrottle.LOGIN_IP, ip)) {
			long minutes = Math.max(throttle.retryAfterMinutes(RequestThrottle.LOGIN_USER, userKey),
					throttle.retryAfterMinutes(RequestThrottle.LOGIN_IP, ip));
			model.addAttribute("error", "Too many failed login attempts. Try again in " + minutes + " minute(s).");
			model.addAttribute("username", username);
			return "login";
		}

		if (adminService.authenticate(username, password)) {
			throttle.clear(RequestThrottle.LOGIN_USER, userKey);
			// New session id after login (protects against session fixation)
			request.changeSessionId();
			session.setAttribute("adminUsername", username);
			auditService.record(AuditService.LOGIN_SUCCESS, "Admin", username, "Logged in");
			return "redirect:/";
		}

		throttle.record(RequestThrottle.LOGIN_USER, userKey);
		throttle.record(RequestThrottle.LOGIN_IP, ip);

		// Never log the password that was typed - only the username that was tried
		auditService.recordAs("anonymous", AuditService.LOGIN_FAILED, "Admin", username,
				"Failed login attempt for username '" + username + "'");

		model.addAttribute("error", "Invalid username or password");
		model.addAttribute("username", username);
		return "login";
	}

	// Change Password (logged-in admins only - protected by LoginInterceptor)
	@GetMapping("/change-password")
	public String changePasswordPage() {
		return "change-password";
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam String currentPassword,
			@RequestParam String newPassword,
			@RequestParam String confirmPassword,
			HttpSession session,
			Model model) {

		String username = String.valueOf(session.getAttribute("adminUsername"));

		if (throttle.isBlocked(RequestThrottle.LOGIN_USER, username)) {
			model.addAttribute("error", "Too many failed attempts. Try again in "
					+ throttle.retryAfterMinutes(RequestThrottle.LOGIN_USER, username) + " minute(s).");
			return "change-password";
		}

		if (newPassword.length() < MIN_PASSWORD_LENGTH) {
			model.addAttribute("error", "New password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
		} else if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
			model.addAttribute("error", "New password is too long (maximum 72 bytes).");
		} else if (!newPassword.equals(confirmPassword)) {
			model.addAttribute("error", "New password and confirmation do not match.");
		} else if (newPassword.equals(currentPassword)) {
			model.addAttribute("error", "New password must be different from the current one.");
		} else if (!adminService.changePassword(username, currentPassword, newPassword)) {
			throttle.record(RequestThrottle.LOGIN_USER, username);
			model.addAttribute("error", "Current password is incorrect.");
		} else {
			throttle.clear(RequestThrottle.LOGIN_USER, username);
			auditService.record(AuditService.PASSWORD_CHANGED, "Admin", username, "Password changed");
			model.addAttribute("success", "Password changed successfully.");
		}
		return "change-password";
	}

	// Logout
	@PostMapping("/logout")
	public String logout(HttpSession session) {
		// Record it first - once the session is invalidated we no longer know who it was
		if (session.getAttribute("adminUsername") != null) {
			auditService.record(AuditService.LOGOUT, "Admin", session.getAttribute("adminUsername"), "Logged out");
		}
		session.invalidate();
		return "redirect:/login";
	}
}
