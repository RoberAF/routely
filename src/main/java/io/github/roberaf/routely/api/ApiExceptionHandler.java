package io.github.roberaf.routely.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import io.github.roberaf.routely.service.NotFoundException;
import io.github.roberaf.routely.service.PlanAccessDeniedException;
import jakarta.validation.ConstraintViolationException;

/**
 * Every error this API returns is RFC 7807 - {@code application/problem+json},
 * never a bare stack trace or Spring's default whitelabel body.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			errors.put(fieldError.getField(), fieldError.getDefaultMessage());
		}

		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Request body failed validation");
		problemDetail.setTitle("Validation failed");
		problemDetail.setProperty("errors", errors);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
	}

	@ExceptionHandler(NotFoundException.class)
	public ProblemDetail handleNotFound(NotFoundException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
		problemDetail.setTitle("Not found");
		return problemDetail;
	}

	@ExceptionHandler({ IllegalArgumentException.class, ConstraintViolationException.class,
			MethodArgumentTypeMismatchException.class })
	public ProblemDetail handleBadRequest(Exception exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				exception.getMessage());
		problemDetail.setTitle("Bad request");
		return problemDetail;
	}

	@ExceptionHandler(PlanAccessDeniedException.class)
	public ProblemDetail handleForbidden(PlanAccessDeniedException exception) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
		problemDetail.setTitle("Forbidden");
		return problemDetail;
	}
}
