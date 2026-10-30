package com.student.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RequestThrottleTest {

	@Test
	void blocksAfterFiveFailuresAndClearsOnSuccess() {
		RequestThrottle throttle = new RequestThrottle();

		for (int i = 0; i < 4; i++) {
			throttle.record(RequestThrottle.LOGIN_USER, "Admin");
		}
		assertFalse(throttle.isBlocked(RequestThrottle.LOGIN_USER, "admin"));

		throttle.record(RequestThrottle.LOGIN_USER, "ADMIN");
		assertTrue(throttle.isBlocked(RequestThrottle.LOGIN_USER, "admin"));

		throttle.clear(RequestThrottle.LOGIN_USER, "admin");
		assertFalse(throttle.isBlocked(RequestThrottle.LOGIN_USER, "admin"));
	}
	
	@Test
	void sameUsernameFromDifferentAddressesIsCountedSeparately() {
		RequestThrottle throttle = new RequestThrottle();
		for (int i = 0; i < 5; i++) {
			throttle.record(RequestThrottle.LOGIN_USER, "admin|10.0.0.1");
		}
		assertTrue(throttle.isBlocked(RequestThrottle.LOGIN_USER, "admin|10.0.0.1"));
		assertFalse(throttle.isBlocked(RequestThrottle.LOGIN_USER, "admin|10.0.0.2"));
	}
}
