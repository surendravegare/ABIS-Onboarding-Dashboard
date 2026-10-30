package com.student.config;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.student.service.AdminService;

// Runs once at application startup. If the "admins" table is empty (e.g. on a
// brand new database) it creates the first admin so the login page is usable.
//
// The account comes from the ADMIN_USERNAME / ADMIN_PASSWORD environment
// variables. If ADMIN_PASSWORD is not set, a random password is generated and
// printed ONCE in the console - there is no fixed default password any more.
// Passwords are stored as BCrypt hashes; change yours from the dashboard
// ("Change Password").
@Component
public class DataSeeder implements CommandLineRunner {

	private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

	@Autowired
	private AdminService adminService;

	@Value("${app.admin.username:admin}")
	private String username;

	@Value("${app.admin.password:}")
	private String configuredPassword;

	@Override
	public void run(String... args) {
		if (adminService.anyAdminExists()) {
			return;
		}

		boolean generated = configuredPassword == null || configuredPassword.isBlank();
		String password = generated ? randomPassword(16) : configuredPassword;

		adminService.registerAdmin(username, password);

		System.out.println("======================================================");
		System.out.println(" No admin found - created the first admin account:");
		System.out.println("   username: " + username);
		if (generated) {
			System.out.println("   password: " + password + "   (generated - shown only this once)");
		} else {
			System.out.println("   password: taken from ADMIN_PASSWORD");
		}
		System.out.println(" Change it after logging in (Change Password).");
		System.out.println("======================================================");
	}

	private String randomPassword(int length) {
		SecureRandom random = new SecureRandom();
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
		}
		return sb.toString();
	}
}
