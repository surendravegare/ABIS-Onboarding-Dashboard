package com.student.exception;

// A problem whose message is safe to show to the person using the app
// (validation errors, duplicate email, rejected file type ...).
public class UserFacingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UserFacingException(String message) {
		super(message);
	}
}
