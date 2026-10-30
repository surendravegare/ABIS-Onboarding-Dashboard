package com.student.config;

import java.time.Year;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// One place that holds the dropdown choices used by the register, add and edit
// student forms. Change a list here and all three forms update.
@ControllerAdvice
public class FormOptions {

	public static final List<String> COURSES = List.of(
			"BA", "B.Com", "B.Sc", "BBA", "BCA", "BCS", "B.E", "B.Tech",
			"MA", "M.Com", "M.Sc", "MBA", "MCA", "M.E", "M.Tech", "Diploma");

	// Same list as "Domains Available" on the welcome page
	public static final List<String> DOMAINS = List.of(
			"Java Developer", "Web Developer", "Cyber Security", "Cloud Computing",
			"Data Analysis", "Data Science", "AI/ML", "Python", "Business Analysis",
			"HR", "Finance", "Marketing", "Operations", "Graphic Design");

	@ModelAttribute("courseOptions")
	public List<String> courseOptions() {
		return COURSES;
	}

	@ModelAttribute("domainOptions")
	public List<String> domainOptions() {
		return DOMAINS;
	}

	// 15..60 matches the limits in Student / StudentValidator
	@ModelAttribute("ageOptions")
	public List<Integer> ageOptions() {
		return IntStream.rangeClosed(15, 60).boxed().toList();
	}

	// Newest first: (this year + 6) down to 1990
	@ModelAttribute("yearOptions")
	public List<Integer> yearOptions() {
		int top = Year.now().getValue() + 6;
		return IntStream.rangeClosed(1990, top).map(i -> top - (i - 1990)).boxed().toList();
	}
}