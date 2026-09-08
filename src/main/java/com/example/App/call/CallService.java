package com.example.App.call;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import com.example.App.auth.User;
import com.example.App.common.ApiException;
import com.example.App.config.TwilioProperties;
import com.example.App.contact.Contact;
import com.twilio.http.HttpMethod;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.type.PhoneNumber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Places outbound calls.
 *
 * The flow, which is worth holding in your head while reading this:
 *   1. we save a CallRecord so the row id can go in the webhook URLs
 *   2. we ask Twilio to ring the *user's own* phone (leg 1)
 *   3. when the user answers, Twilio fetches /twilio/voice/{id} asking what to do
 *   4. that endpoint returns TwiML telling Twilio to dial the contact (leg 2) and bridge them
 *   5. Twilio posts the outcome to /twilio/status/{id}, which updates this row
 */
@Service
public class CallService {

	private static final Logger log = LoggerFactory.getLogger(CallService.class);

	private final CallRepository calls;
	private final TwilioProperties props;

	public CallService(CallRepository calls, TwilioProperties props) {
		this.calls = calls;
		this.props = props;
	}

	/**
	 * Dials any number. The contact is optional - pass null for a stranger.
	 *
	 * @param callbackForCallId the inbound call being returned, or null
	 */
	public CallRecord placeCall(User user, String toNumber, Contact contact, Long callbackForCallId) {
		requireReadyToCall();

		CallRecord record = CallRecord.outbound(
				user.getId(), toNumber, user.getPhoneNumber(), props.getFromNumber());
		if (contact != null) {
			record.setContactId(contact.getId());
			record.setContactName(contact.getName());
		}
		record.setCallbackForCallId(callbackForCallId);
		calls.save(record);

		try {
			Call call = Call.creator(
							new PhoneNumber(user.getPhoneNumber()),      // to: ring the agent first
							new PhoneNumber(props.getFromNumber()),      // from: the Twilio number you own
							URI.create(props.getPublicBaseUrl() + "/twilio/voice/" + record.getId()))
					.setStatusCallback(URI.create(props.getPublicBaseUrl() + "/twilio/status/" + record.getId()))
					.setStatusCallbackEvent(List.of("initiated", "ringing", "answered", "completed"))
					.setStatusCallbackMethod(HttpMethod.POST)
					.setTimeout(30)
					.create();

			record.setCallSid(call.getSid());
			record.setStatus(call.getStatus() == null ? "queued" : call.getStatus().toString());
			log.info("Call {} placed: {} -> {} (sid {})",
					record.getId(), user.getPhoneNumber(), toNumber, call.getSid());

		} catch (com.twilio.exception.ApiException ex) {
			record.setStatus("failed");
			record.setErrorMessage(ex.getMessage());
			record.setCompletedAt(Instant.now());
			calls.save(record);
			log.warn("Twilio rejected call {} (code {}): {}", record.getId(), ex.getCode(), ex.getMessage());
			throw new ApiException(explain(ex));
		}

		return calls.save(record);
	}

	/** Builds the TwiML that connects the two legs. Called by the /twilio/voice webhook. */
	public String twimlFor(CallRecord record) {
		return TwiML.connect(record.displayName(), record.getToNumber(), props.getFromNumber());
	}

	private void requireReadyToCall() {
		if (!props.isConfigured()) {
			throw new ApiException("Twilio is not configured on the server. "
					+ "Set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN and TWILIO_FROM_NUMBER, then restart.");
		}
		if (props.getPublicBaseUrl().isBlank()) {
			// Without this Twilio has nowhere to ask what to do, so the call would connect to silence.
			throw new ApiException("No public URL is set, so Twilio cannot reach this server. "
					+ "Run 'ngrok http 8080' and set APP_PUBLIC_BASE_URL to the https URL it prints.");
		}
	}

	/** Turns the better-known Twilio error codes into something a user can act on. */
	private static String explain(com.twilio.exception.ApiException ex) {
		Integer code = ex.getCode();
		if (code == null) {
			return "Twilio could not place the call: " + ex.getMessage();
		}
		return switch (code) {
			case 21215 -> "Calling that country is blocked on this Twilio account. "
					+ "Enable it under Voice > Settings > Geo Permissions.";
			case 21210, 21211 -> "That phone number was rejected by Twilio. It must be in E.164 form, like +15551234567.";
			case 21219, 21608 -> "That number is not verified. A trial account can only call verified numbers - "
					+ "add it under Phone Numbers > Verified Caller IDs.";
			case 21212, 21213 -> "The 'from' number is not valid for this account. "
					+ "It must be a Twilio number you own, in E.164 form.";
			case 20003 -> "Twilio rejected the credentials. Check the Account SID and Auth Token.";
			case 21606 -> "The 'from' number cannot make outbound calls. Check it is voice-capable.";
			default -> "Twilio could not place the call (error " + code + "): " + ex.getMessage();
		};
	}
}
