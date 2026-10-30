package com.student.config;

import jakarta.servlet.http.HttpServletRequest;

// Same rule the audit log uses: prefer the proxy's X-Forwarded-For header,
// otherwise fall back to the direct connection address.
public final class ClientIp {

	private ClientIp() {
	}

	public static String of(HttpServletRequest request) {
		if (request == null) {
			return "unknown";
		}
	    String ip = request.getRemoteAddr();
	    return ip == null ? "unknown" : ip;
	}
}
