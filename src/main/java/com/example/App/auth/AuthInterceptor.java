package com.example.App.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Bounces logged-out visitors: JSON 401 for API calls, a redirect to the login page for HTML.
 *
 * The Twilio webhooks under /twilio/** are deliberately not covered here - Twilio has no session.
 * They are authenticated by their request signature instead, in TwilioSignatureInterceptor.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		HttpSession session = request.getSession(false);
		if (session != null && session.getAttribute(CurrentUser.SESSION_KEY) != null) {
			return true;
		}

		if (request.getRequestURI().startsWith("/api/")) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json");
			response.getWriter().write("{\"error\":\"Please log in\"}");
		} else {
			response.sendRedirect("/login.html");
		}
		return false;
	}
}
