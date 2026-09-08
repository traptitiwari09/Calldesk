package com.example.App.call;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.App.contact.ContactRepository;


@RestController
@RequestMapping("/twilio")
public class TwilioWebhookController {

	private static final Logger log = LoggerFactory.getLogger(TwilioWebhookController.class);

	private final CallRepository calls;
	private final CallService callService;
	private final ContactRepository contacts;

	public TwilioWebhookController(CallRepository calls, CallService callService, ContactRepository contacts) {
		this.calls = calls;
		this.callService = callService;
		this.contacts = contacts;
	}

	
	@PostMapping(value = "/incoming", produces = MediaType.APPLICATION_XML_VALUE)
	public String incoming(@RequestParam(name = "From", required = false) String from,
			@RequestParam(name = "To", required = false) String to,
			@RequestParam(name = "CallSid", required = false) String callSid) {

		if (from == null || from.isBlank()) {
			log.warn("Inbound webhook with no From - ignoring");
			return TwiML.saySomethingWentWrong("Sorry, something went wrong. Goodbye.");
		}

		CallRecord record = CallRecord.inbound(from, to, callSid);

		
		contacts.findFirstByPhoneNumber(from).ifPresent(contact -> {
			record.setContactId(contact.getId());
			record.setContactName(contact.getName());
		});

		calls.save(record);
		log.info("Inbound call {} from {} ({})", record.getId(), from,
				record.getContactName() == null ? "unknown" : record.getContactName());

		return TwiML.acknowledgeInbound();
	}

	
	@PostMapping(value = "/voice/{callId}", produces = MediaType.APPLICATION_XML_VALUE)
	public String voice(@PathVariable Long callId) {
		return calls.findById(callId)
				.map(record -> {
					log.info("Voice webhook for call {} - dialling {}", callId, record.getToNumber());
					return callService.twimlFor(record);
				})
				.orElseGet(() -> {
					log.warn("Voice webhook for unknown call {}", callId);
					return TwiML.saySomethingWentWrong("Sorry, this call is no longer available. Goodbye.");
				});
	}

	
	@PostMapping("/status/{callId}")
	public void status(@PathVariable Long callId,
			@RequestParam(name = "CallStatus", required = false) String callStatus,
			@RequestParam(name = "CallSid", required = false) String callSid,
			@RequestParam(name = "CallDuration", required = false) String callDuration,
			@RequestParam(name = "ErrorMessage", required = false) String errorMessage) {

		calls.findById(callId).ifPresentOrElse(record -> {
			if (callStatus != null) {
				record.setStatus(callStatus);
			}
			if (callSid != null && record.getCallSid() == null) {
				record.setCallSid(callSid);
			}
			if (callDuration != null && !callDuration.isBlank()) {
				try {
					record.setDurationSeconds(Integer.parseInt(callDuration));
				} catch (NumberFormatException ignored) {
					// Twilio sends "" on the non-final events; nothing to record yet.
				}
			}
			if (errorMessage != null && !errorMessage.isBlank()) {
				record.setErrorMessage(errorMessage);
			}
			if (record.isFinished() && record.getCompletedAt() == null) {
				record.setCompletedAt(Instant.now());
			}
			calls.save(record);
			log.info("Call {} is now {}{}", callId, callStatus,
					record.getDurationSeconds() == null ? "" : " (" + record.getDurationSeconds() + "s)");
		}, () -> log.warn("Status webhook for unknown call {}", callId));
	}
}
