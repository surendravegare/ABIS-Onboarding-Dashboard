package com.student.exception;

// Thrown when a record (for example a student ID) does not exist.
public class ResourceNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ResourceNotFoundException(String message) {
		super(message);
	}
}
