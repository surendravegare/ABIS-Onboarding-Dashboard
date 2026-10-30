package com.student.service;

import com.student.entity.Admin;

public interface AdminService {

	// Creates a new admin account with the given username/password.
	// The password is hashed (BCrypt) before being stored in the database.
	Admin registerAdmin(String username, String rawPassword);

	// Verifies the given username/password against the stored (hashed) credentials.
	boolean authenticate(String username, String rawPassword);

	// Changes the password after re-checking the current one. Returns false if the current password is wrong.
	boolean changePassword(String username, String currentPassword, String newPassword);

	boolean usernameExists(String username);

	boolean anyAdminExists();
}
