package com.example.App.auth;

import java.util.Map;

import com.example.App.common.ApiException;
import com.example.App.common.PhoneNumbers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UserRepository users;
	private final CurrentUser currentUser;
	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	private final String defaultCountryCode;

	public AuthController(UserRepository users, CurrentUser currentUser,
			@Value("${app.default-country-code:+1}") String defaultCountryCode) {
		this.users = users;
		this.currentUser = currentUser;
		this.defaultCountryCode = defaultCountryCode;
	}

	public record SignupRequest(String email, String password, String name, String phoneNumber) {
	}

	public record LoginRequest(String email, String password) {
	}

	@PostMapping("/signup")
	public Map<String, Object> signup(@RequestBody SignupRequest body, HttpServletRequest request) {
		String email = body.email() == null ? "" : body.email().trim().toLowerCase();
		String password = body.password() == null ? "" : body.password();

		if (email.isBlank() || !email.contains("@")) {
			throw new ApiException("Enter a valid email address");
		}
		if (password.length() < 8) {
			throw new ApiException("Password must be at least 8 characters");
		}
		if (users.existsByEmailIgnoreCase(email)) {
			throw new ApiException("That email is already registered");
		}

		// This is the phone that rings when the user places a call, so it has to be reachable.
		String phone = PhoneNumbers.toE164(body.phoneNumber(), defaultCountryCode);

		User user = users.save(new User(email, encoder.encode(password), safeName(body.name(), email), phone));
		startSession(request, user);
		return describe(user);
	}

	@PostMapping("/login")
	public Map<String, Object> login(@RequestBody LoginRequest body, HttpServletRequest request) {
		String email = body.email() == null ? "" : body.email().trim();
		String password = body.password() == null ? "" : body.password();

		User user = users.findByEmailIgnoreCase(email)
				// Same message either way, so this cannot be used to discover which emails exist.
				.orElseThrow(() -> new ApiException("Email or password is incorrect", HttpStatus.UNAUTHORIZED));

		if (!encoder.matches(password, user.getPasswordHash())) {
			throw new ApiException("Email or password is incorrect", HttpStatus.UNAUTHORIZED);
		}

		startSession(request, user);
		return describe(user);
	}

	@PostMapping("/logout")
	public Map<String, Object> logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return Map.of("ok", true);
	}

	/** Used by every page on load to decide whether to redirect to login. */
	@GetMapping("/me")
	public Map<String, Object> me(HttpServletRequest request) {
		return describe(currentUser.require(request));
	}

	private void startSession(HttpServletRequest request, User user) {
		HttpSession existing = request.getSession(false);
		if (existing != null) {
			// New login, new session id - stops a pre-set cookie from being reused (session fixation).
			existing.invalidate();
		}
		request.getSession(true).setAttribute(CurrentUser.SESSION_KEY, user.getId());
	}

	private static Map<String, Object> describe(User user) {
		return Map.of(
				"id", user.getId(),
				"email", user.getEmail(),
				"name", user.getName(),
				"phoneNumber", user.getPhoneNumber());
	}

	private static String safeName(String name, String email) {
		if (name == null || name.isBlank()) {
			return email.substring(0, email.indexOf('@'));
		}
		return name.trim();
	}
}
