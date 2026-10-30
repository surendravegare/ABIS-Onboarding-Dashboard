package com.student.service;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.student.entity.Student;
import com.student.exception.UserFacingException;
import com.student.repository.StudentRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

// Server-side checks for every student that is added, registered or edited.
// The browser-side "required"/"pattern" attributes are only a convenience.
@Component
public class StudentValidator {

	@Autowired
	private Validator validator;

	@Autowired
	private StudentRepository studentRepository;

	// Trims the text fields, then throws UserFacingException listing every problem found.
	public void validate(Student student) {

		student.setName(trim(student.getName()));
		student.setCourse(trim(student.getCourse()));
		student.setEmail(trim(student.getEmail()));
		student.setPhone(trim(student.getPhone()));
		student.setDomain(trim(student.getDomain()));
		student.setCollegeName(trim(student.getCollegeName()));

		// TreeSet: sorted and without duplicate messages
		TreeSet<String> problems = new TreeSet<>();
		for (ConstraintViolation<Student> violation : validator.validate(student)) {
			problems.add(violation.getMessage());
		}

		if (student.getTotalFees() != null && student.getFeesPaid() != null
				&& student.getFeesPaid().compareTo(student.getTotalFees()) > 0) {
			problems.add("Fees paid cannot be more than total fees");
		}

		List<String> all = new ArrayList<>(problems);

		// Duplicate checks only make sense once the basic fields are valid
		if (all.isEmpty()) {
			if (studentRepository.existsByEmailIgnoreCaseAndIdNot(student.getEmail(), student.getId())) {
				all.add("A student with this email already exists");
			}
			if (studentRepository.existsByPhoneAndIdNot(student.getPhone(), student.getId())) {
				all.add("A student with this phone number already exists");
			}
		}

		if (!all.isEmpty()) {
			throw new UserFacingException(String.join("; ", all) + ".");
		}
	}

	private String trim(String value) {
		return value == null ? null : value.trim();
	}
}
