package ru.dantalian.copvoc.web.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "page not found")
public class PageNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PageNotFoundException() {
		super();
	}

	public PageNotFoundException(final String aMessage, final Throwable aCause) {
		super(aMessage, aCause);
	}

	public PageNotFoundException(final String aMessage) {
		super(aMessage);
	}

}
