package com.example.App.call;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.App.auth.CurrentUser;
import com.example.App.auth.User;
import com.example.App.common.ApiException;
import com.example.App.common.PhoneNumbers;
import com.example.App.contact.Contact;
import com.example.App.contact.ContactRepository;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/calls")
public class CallController {

	private final CallService callService;
	private final CallRepository calls;
	private final ContactRepository contacts;
	private final CurrentUser currentUser;

	private final String defaultCountryCode;

	public CallController(CallService callService, CallRepository calls,
			ContactRepository contacts, CurrentUser currentUser,
			@org.springframework.beans.factory.annotation.Value("${app.default-country-code:+1}") String defaultCountryCode) {
		this.callService = callService;
		this.calls = calls;
		this.contacts = contacts;
		this.currentUser = currentUser;
		this.defaultCountryCode = defaultCountryCode;
	}

	
	public record PlaceCallRequest(Long contactId, String phoneNumber, Long callbackForCallId, String firstLegNumber) {
	}


	@PostMapping
	public Map<String, Object> place(@RequestBody PlaceCallRequest body, HttpServletRequest request) {
		User user = currentUser.require(request);

		Contact contact = null;
		String toNumber;
		Long callbackFor = body.callbackForCallId();

		// Held until the call is actually accepted by Twilio. Claiming it earlier means a
		// rejected call leaves the caller marked "returned" - out of everyone's queue, and
		// never rung back. Losing a caller is the exact failure this app exists to prevent.
		CallRecord inboundToClaim = null;

		if (callbackFor != null) {
			// Returning a call somebody made to us.
			CallRecord inbound = calls.findById(callbackFor)
					.filter(CallRecord::isInbound)
					.orElseThrow(() -> new ApiException("That call is not in the queue", HttpStatus.NOT_FOUND));
			toNumber = inbound.getToNumber();
			if (inbound.getContactId() != null) {
				contact = contacts.findByIdAndUserId(inbound.getContactId(), user.getId()).orElse(null);
			}
			inboundToClaim = inbound;

		} else if (body.contactId() != null) {
			contact = contacts.findByIdAndUserId(body.contactId(), user.getId())
					.orElseThrow(() -> new ApiException("Contact not found", HttpStatus.NOT_FOUND));
			toNumber = contact.getPhoneNumber();

		} else if (body.phoneNumber() != null && !body.phoneNumber().isBlank()) {
			// A number nobody has saved. This is the normal case at any real volume.
			toNumber = PhoneNumbers.toE164(body.phoneNumber(), defaultCountryCode);
			contact = contacts.findFirstByPhoneNumber(toNumber)
					.filter(c -> c.getUserId().equals(user.getId()))
					.orElse(null);

		} else {
			throw new ApiException("Pick someone to call, or type a number");
		}

		// Leg 1 defaults to the agent's own phone, so the dashboard and the callback
		// queue - which never send this field - keep working unchanged.
		String firstLeg = (body.firstLegNumber() == null || body.firstLegNumber().isBlank())
				? user.getPhoneNumber()
				: PhoneNumbers.toE164(body.firstLegNumber(), defaultCountryCode);

		// Twilio would ring the number, then try to dial it again while it is busy:
		// leg 2 fails and leg 1 is billed for nothing.
		if (firstLeg.equals(toNumber)) {
			throw new ApiException("Both numbers are the same - pick two different people");
		}

		CallRecord placed = callService.placeCall(user, toNumber, contact, callbackFor, firstLeg);

		// Only now is the caller genuinely being rung back, so only now leave the queue.
		if (inboundToClaim != null) {
			inboundToClaim.setUserId(user.getId());
			inboundToClaim.setStatus("returned");
			calls.save(inboundToClaim);
		}

		return describe(placed);
	}

	
	@GetMapping("/inbound")
	public List<Map<String, Object>> inbound(HttpServletRequest request) {
		currentUser.require(request);
		return calls.findTop100ByDirectionOrderByCreatedAtDesc("INBOUND").stream()
				.map(CallController::describe)
				.toList();
	}

	
		@GetMapping
	public List<Map<String, Object>> history(
			@RequestParam(required = false) String direction,
			HttpServletRequest request) {
		User user = currentUser.require(request);

		// Filtering in the query rather than in the browser: the list is capped at
		// 100 rows, so a client-side filter would silently drop older calls of the direction
		
		List<CallRecord> found = switch (direction == null ? "ALL" : direction.toUpperCase()) {
			case "ALL" -> calls.findTop100ByUserIdOrDirectionOrderByCreatedAtDesc(user.getId(), "INBOUND");
			case "OUTBOUND" -> calls.findTop100ByUserIdAndDirectionOrderByCreatedAtDesc(user.getId(), "OUTBOUND");
			case "INBOUND" -> calls.findTop100ByDirectionOrderByCreatedAtDesc("INBOUND");
			default -> throw new ApiException("Unknown direction: " + direction);
		};

		return found.stream().map(CallController::describe).toList();
	}


	
	@GetMapping("/{id}")
	public Map<String, Object> one(@PathVariable Long id, HttpServletRequest request) {
		User user = currentUser.require(request);
		return calls.findByIdAndUserId(id, user.getId())
				.map(CallController::describe)
				.orElseThrow(() -> new ApiException("Call not found", HttpStatus.NOT_FOUND));
	}

	private static Map<String, Object> describe(CallRecord record) {
		// HashMap rather than Map.of - several of these are legitimately null.
		Map<String, Object> out = new HashMap<>();
		out.put("id", record.getId());
		out.put("contactId", record.getContactId());
		out.put("contactName", record.getContactName());
		out.put("displayName", record.displayName());
		out.put("direction", record.getDirection());
		out.put("inbound", record.isInbound());
		out.put("twilioNumber", record.getTwilioNumber());
		out.put("callbackForCallId", record.getCallbackForCallId());
		out.put("toNumber", record.getToNumber());
		out.put("displayNumber", PhoneNumbers.forDisplay(record.getToNumber()));

		// Both ends of the call. Outbound bridges the agent's phone to the other party;
		// inbound is the caller reaching our Twilio number (nobody is bridged in - it queues).
		String from = record.isInbound() ? record.getToNumber() : record.getAgentNumber();
		String to = record.isInbound() ? record.getTwilioNumber() : record.getToNumber();
		out.put("agentNumber", record.getAgentNumber());
		out.put("fromNumber", from);
		out.put("displayFrom", PhoneNumbers.forDisplay(from));
		out.put("displayTo", PhoneNumbers.forDisplay(to));
		out.put("status", record.getStatus());
		out.put("finished", record.isFinished());
		out.put("durationSeconds", record.getDurationSeconds());
		out.put("errorMessage", record.getErrorMessage());
		out.put("createdAt", record.getCreatedAt().toString());
		out.put("completedAt", record.getCompletedAt() == null ? null : record.getCompletedAt().toString());
		return out;
	}
}
