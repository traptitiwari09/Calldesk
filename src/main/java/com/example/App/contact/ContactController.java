package com.example.App.contact;

import java.util.List;
import java.util.Map;

import com.example.App.auth.CurrentUser;
import com.example.App.auth.User;
import com.example.App.common.ApiException;
import com.example.App.common.PhoneNumbers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

	private final ContactRepository contacts;
	private final CurrentUser currentUser;
	private final String defaultCountryCode;

	public ContactController(ContactRepository contacts, CurrentUser currentUser,
			@Value("${app.default-country-code:+1}") String defaultCountryCode) {
		this.contacts = contacts;
		this.currentUser = currentUser;
		this.defaultCountryCode = defaultCountryCode;
	}

	public record ContactRequest(String name, String phoneNumber) {
	}

	@GetMapping
	public List<Map<String, Object>> list(HttpServletRequest request) {
		User user = currentUser.require(request);
		return contacts.findByUserIdOrderByNameAsc(user.getId()).stream()
				.map(ContactController::describe)
				.toList();
	}

	@PostMapping
	public Map<String, Object> add(@RequestBody ContactRequest body, HttpServletRequest request) {
		User user = currentUser.require(request);

		String name = body.name() == null ? "" : body.name().trim();
		if (name.isBlank()) {
			throw new ApiException("Contact name is required");
		}

		String phone = PhoneNumbers.toE164(body.phoneNumber(), defaultCountryCode);
		if (contacts.existsByUserIdAndPhoneNumber(user.getId(), phone)) {
			throw new ApiException("You already have a contact with that number");
		}

		return describe(contacts.save(new Contact(user.getId(), name, phone)));
	}

	@DeleteMapping("/{id}")
	public Map<String, Object> delete(@PathVariable Long id, HttpServletRequest request) {
		User user = currentUser.require(request);
		Contact contact = contacts.findByIdAndUserId(id, user.getId())
				.orElseThrow(() -> new ApiException("Contact not found", HttpStatus.NOT_FOUND));
		contacts.delete(contact);
		return Map.of("ok", true);
	}

	static Map<String, Object> describe(Contact contact) {
		return Map.of(
				"id", contact.getId(),
				"name", contact.getName(),
				"phoneNumber", contact.getPhoneNumber(),
				"displayNumber", PhoneNumbers.forDisplay(contact.getPhoneNumber()));
	}
}
