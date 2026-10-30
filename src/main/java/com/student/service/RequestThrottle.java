package com.student.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

// Small in-memory "too many attempts" guard (login failures, public registrations).
// Counters reset when the application restarts.
@Component
public class RequestThrottle {

	public static final String LOGIN_USER = "login-user";
	public static final String LOGIN_IP = "login-ip";
	public static final String REGISTER_IP = "register-ip";

	private static final long MINUTE = 60_000L;

	private record Limit(int max, long windowMillis) {
	}

	private final Map<String, Limit> limits = Map.of(
			LOGIN_USER, new Limit(5, 15 * MINUTE),
			LOGIN_IP, new Limit(20, 15 * MINUTE),
			REGISTER_IP, new Limit(10, 60 * MINUTE));

	private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

	public boolean isBlocked(String bucket, String key) {
		Deque<Long> deque = hits.get(id(bucket, key));
		if (deque == null) {
			return false;
		}
		Limit limit = limits.get(bucket);
		synchronized (deque) {
			prune(deque, limit);
			return deque.size() >= limit.max();
		}
	}

	public void record(String bucket, String key) {
		Deque<Long> deque = hits.computeIfAbsent(id(bucket, key), k -> new ArrayDeque<>());
		Limit limit = limits.get(bucket);
		synchronized (deque) {
			prune(deque, limit);
			deque.addLast(System.currentTimeMillis());
		}
		if (hits.size() > 10_000) {
			cleanup();
		}
	}

	public void clear(String bucket, String key) {
		hits.remove(id(bucket, key));
	}

	// Minutes (rounded up) until the oldest counted attempt expires
	public long retryAfterMinutes(String bucket, String key) {
		Deque<Long> deque = hits.get(id(bucket, key));
		if (deque == null) {
			return 0;
		}
		Limit limit = limits.get(bucket);
		synchronized (deque) {
			prune(deque, limit);
			if (deque.isEmpty()) {
				return 0;
			}
			long remaining = deque.peekFirst() + limit.windowMillis() - System.currentTimeMillis();
			return Math.max(1, (remaining + MINUTE - 1) / MINUTE);
		}
	}

	private void prune(Deque<Long> deque, Limit limit) {
		long cutoff = System.currentTimeMillis() - limit.windowMillis();
		while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
			deque.pollFirst();
		}
	}

	private void cleanup() {
		hits.entrySet().removeIf(e -> {
			String bucket = e.getKey().substring(0, e.getKey().indexOf('|'));
			Deque<Long> deque = e.getValue();
			synchronized (deque) {
				prune(deque, limits.get(bucket));
				return deque.isEmpty();
			}
		});
	}

	private String id(String bucket, String key) {
		return bucket + "|" + (key == null ? "" : key.toLowerCase());
	}
}
