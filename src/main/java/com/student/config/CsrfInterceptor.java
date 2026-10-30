package com.student.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// Every page gets a per-session token (session attribute "csrfToken", available
// in templates as ${session.csrfToken}). Every state-changing request must send
// it back in a form field named "_csrf", otherwise it is rejected with 403.
public class CsrfInterceptor implements HandlerInterceptor {

	public static final String SESSION_KEY = "csrfToken";
	public static final String FIELD_NAME = "_csrf";

	private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
	private static final SecureRandom RANDOM = new SecureRandom();

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		HttpSession session = request.getSession(true);
		String expected = (String) session.getAttribute(SESSION_KEY);
		if (expected == null) {
			byte[] bytes = new byte[32];
			RANDOM.nextBytes(bytes);
			expected = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
			session.setAttribute(SESSION_KEY, expected);
		}

		if (SAFE_METHODS.contains(request.getMethod())) {
			return true;
		}

		String actual = request.getParameter(FIELD_NAME);
		boolean valid = actual != null && MessageDigest.isEqual(
				expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));

		if (!valid) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing security token");
			return false;
		}
		return true;
	}
}
