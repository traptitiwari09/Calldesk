package com.example.App.common;

import org.springframework.http.HttpStatus;

/** An error with a message that is safe to show the user. */
public class ApiException extends RuntimeException {

	private final HttpStatus status;

	public ApiException(String message) {
		this(message, HttpStatus.BAD_REQUEST);
	}

	public ApiException(String message, HttpStatus status) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
