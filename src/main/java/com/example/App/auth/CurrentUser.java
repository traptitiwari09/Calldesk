package com.example.App.auth;

import com.example.App.common.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Reads the logged-in user out of the HttpSession. */
@Component
public class CurrentUser {

	static final String SESSION_KEY = "userId";

	private final UserRepository users;

	public CurrentUser(UserRepository users) {
		this.users = users;
	}

	/** @throws ApiException 401 when nobody is logged in. */
	public User require(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		Long userId = session == null ? null : (Long) session.getAttribute(SESSION_KEY);
		if (userId == null) {
			throw new ApiException("Please log in", HttpStatus.UNAUTHORIZED);
		}
		return users.findById(userId)
				.orElseThrow(() -> new ApiException("Please log in", HttpStatus.UNAUTHORIZED));
	}
}
