package com.student.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.student.entity.Admin;
import com.student.repository.AdminRepository;

@Service
public class AdminServiceImpl implements AdminService {

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	private static final String DUMMY_HASH = new BCryptPasswordEncoder().encode("not-a-real-password");

	@Autowired
	private AdminRepository adminRepository;

	@Override
	public Admin registerAdmin(String username, String rawPassword) {
		return adminRepository.save(new Admin(username, encoder.encode(rawPassword)));
	}

	@Override
	public boolean authenticate(String username, String rawPassword) {
		if (username == null || rawPassword == null) {
			return false;
		}
		Optional<Admin> found = adminRepository.findByUsername(username);
		if (found.isEmpty()) {
		    encoder.matches(rawPassword, DUMMY_HASH);   // same cost as a real check
		    return false;
		}
		return passwordMatches(found.get(), rawPassword);
	}

	@Override
	public boolean changePassword(String username, String currentPassword, String newPassword) {
		Optional<Admin> found = adminRepository.findByUsername(username);
		if (found.isEmpty() || !passwordMatches(found.get(), currentPassword)) {
			return false;
		}
		Admin admin = found.get();
		admin.setPassword(encoder.encode(newPassword));
		adminRepository.save(admin);
		return true;
	}

	@Override
	public boolean usernameExists(String username) {
		return adminRepository.existsByUsername(username);
	}

	@Override
	public boolean anyAdminExists() {
		return adminRepository.count() > 0;
	}

	// Checks a BCrypt hash. Accounts created by older versions still hold a plain-text
	// password; those are accepted once and immediately re-saved as a BCrypt hash.
	private boolean passwordMatches(Admin admin, String rawPassword) {
		String stored = admin.getPassword();
		if (stored == null || rawPassword == null) {
			return false;
		}

		try {
			if (stored.startsWith("$2")) {
				return encoder.matches(rawPassword, stored);
			}
		} catch (IllegalArgumentException ex) {
			return false;
		}

		boolean legacyMatch = MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8),
				rawPassword.getBytes(StandardCharsets.UTF_8));
		if (legacyMatch) {
			admin.setPassword(encoder.encode(rawPassword));
			adminRepository.save(admin);
		}
		return legacyMatch;
	}
}
