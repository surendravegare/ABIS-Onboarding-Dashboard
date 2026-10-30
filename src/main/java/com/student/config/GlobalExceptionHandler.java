package com.student.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.student.exception.ResourceNotFoundException;
import com.student.exception.UserFacingException;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String handleNotFound(ResourceNotFoundException ex, Model model) {
		model.addAttribute("message", ex.getMessage());
		return "error-page";
	}

	@ExceptionHandler(UserFacingException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public String handleUserFacing(UserFacingException ex, Model model) {
		model.addAttribute("message", ex.getMessage());
		return "error-page";
	}

	@ExceptionHandler(NoResourceFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String handleNoResource(NoResourceFoundException ex, Model model) {
		model.addAttribute("message", "The page you are looking for was not found.");
		return "error-page";
	}

	@ExceptionHandler({ MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class })
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public String handleBadRequest(Exception ex, Model model) {
		model.addAttribute("message", "That request was not valid. Please go back and try again.");
		return "error-page";
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public String handleTooLarge(MaxUploadSizeExceededException ex, Model model) {
		model.addAttribute("message", "The uploaded file is too large (profile photos can be 50 KB at most).");
		return "error-page";
	}

	// Anything unexpected: the details go to the server log only, never to the browser.
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public String handleAnyException(Exception ex, Model model) {
		log.error("Unhandled exception", ex);
		model.addAttribute("message", "An unexpected error occurred. Please try again.");
		return "error-page";
	}
}