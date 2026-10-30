package com.student.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

class CsrfInterceptorTest {

	private final CsrfInterceptor interceptor = new CsrfInterceptor();

	@Test
	void getRequestsPassAndCreateAToken() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
		assertTrue(interceptor.preHandle(req, new MockHttpServletResponse(), new Object()));
		assertNotNull(req.getSession().getAttribute(CsrfInterceptor.SESSION_KEY));
	}

	@Test
	void postWithoutTokenIsRejected() throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest("POST", "/save");
		MockHttpServletResponse res = new MockHttpServletResponse();
		assertFalse(interceptor.preHandle(req, res, new Object()));
		assertEquals(403, res.getStatus());
	}

	@Test
	void postWithWrongTokenIsRejected() throws Exception {
		MockHttpSession session = new MockHttpSession();
		MockHttpServletRequest get = new MockHttpServletRequest("GET", "/");
		get.setSession(session);
		interceptor.preHandle(get, new MockHttpServletResponse(), new Object());

		MockHttpServletRequest post = new MockHttpServletRequest("POST", "/save");
		post.setSession(session);
		post.setParameter(CsrfInterceptor.FIELD_NAME, "not-the-token");
		MockHttpServletResponse res = new MockHttpServletResponse();
		assertFalse(interceptor.preHandle(post, res, new Object()));
		assertEquals(403, res.getStatus());
	}

	@Test
	void postWithCorrectTokenPasses() throws Exception {
		MockHttpSession session = new MockHttpSession();
		MockHttpServletRequest get = new MockHttpServletRequest("GET", "/");
		get.setSession(session);
		interceptor.preHandle(get, new MockHttpServletResponse(), new Object());
		String token = (String) session.getAttribute(CsrfInterceptor.SESSION_KEY);

		MockHttpServletRequest post = new MockHttpServletRequest("POST", "/save");
		post.setSession(session);
		post.setParameter(CsrfInterceptor.FIELD_NAME, token);
		assertTrue(interceptor.preHandle(post, new MockHttpServletResponse(), new Object()));
	}
}