package com.student.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.student.entity.Student;
import com.student.exception.UserFacingException;
import com.student.repository.StudentRepository;

import jakarta.validation.Validation;

class StudentValidatorTest {

	private StudentRepository repo;
	private StudentValidator sut;

	@BeforeEach
	void setUp() {
		repo = mock(StudentRepository.class);
		sut = new StudentValidator();
		ReflectionTestUtils.setField(sut, "validator",
				Validation.buildDefaultValidatorFactory().getValidator());
		ReflectionTestUtils.setField(sut, "studentRepository", repo);
	}

	private Student valid() {
		Student s = new Student();
		s.setName("Asha");
		s.setAge(21);
		s.setCourse("BCA");
		s.setCgpa(8.0);
		s.setPassoutYear(2025);
		s.setEmail("asha@example.com");
		s.setPhone("9876543210");
		s.setDomain("Java");
		s.setJoiningDate(LocalDate.of(2026, 1, 1));
		s.setCompletionDate(LocalDate.of(2026, 6, 30));
		s.setCollegeName("ABC College");
		s.setTotalFees(new BigDecimal("10000"));
		s.setFeesPaid(new BigDecimal("5000"));
		return s;
	}

	@Test
	void acceptsAValidStudent() {
		assertDoesNotThrow(() -> sut.validate(valid()));
	}

	@Test
	void rejectsAPhoneThatIsNotTenDigits() {
		Student s = valid();
		s.setPhone("12345");
		UserFacingException ex = assertThrows(UserFacingException.class, () -> sut.validate(s));
		assertTrue(ex.getMessage().contains("10 digits"));
	}

	@Test
	void rejectsFeesPaidAboveTotalFees() {
		Student s = valid();
		s.setFeesPaid(new BigDecimal("20000"));
		assertThrows(UserFacingException.class, () -> sut.validate(s));
	}

	@Test
	void rejectsADuplicateEmail() {
		when(repo.existsByEmailIgnoreCaseAndIdNot("asha@example.com", 0)).thenReturn(true);
		assertThrows(UserFacingException.class, () -> sut.validate(valid()));
	}
}