package com.example.App.call;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One row per call attempt.
 *
 * Created before Twilio is contacted, so the row id can go into the webhook URLs;
 * Twilio then tells us which call it is talking about without us having to guess.
 */
@Entity
@Table(name = "calls")
public class CallRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Which agent this call belongs to.
	 *
	 * Null for an inbound call nobody has picked up yet - those sit in a shared
	 * queue that any signed-in agent can claim, which is how a real call centre
	 * distributes work.
	 */
	private Long userId;

	/** OUTBOUND (we placed it) or INBOUND (someone rang our Twilio number). */
	@Column(nullable = false)
	private String direction = "OUTBOUND";

	/**
	 * Set only when the number happens to match a saved contact.
	 *
	 * Deliberately optional: a call is an event involving a phone *number*, and
	 * requiring every unknown caller to be saved first is unworkable at any real
	 * volume. The contact is enrichment, not a prerequisite.
	 */
	private Long contactId;

	/** Copied in, so history still reads after a contact is deleted. Null = unknown caller. */
	private String contactName;

	/** The other party, E.164 - who we called, or who called us. */
	@Column(nullable = false)
	private String toNumber;

	/** The user's own phone - the leg that rings first on an outbound call. */
	private String agentNumber;

	/**
	 * The Twilio number used, recorded per call rather than read from config.
	 *
	 * Config changes; history should not. Once you own a second number, a row
	 * that only points at "whatever is configured now" is a row that lies.
	 */
	private String twilioNumber;

	/** For a callback: the inbound call this one answers. */
	private Long callbackForCallId;

	/** Twilio's id for the call ("CA..."), set once the API accepts it. */
	private String callSid;

	/** queued, initiated, ringing, in-progress, completed, busy, no-answer, failed, canceled. */
	@Column(nullable = false)
	private String status = "queued";

	private Integer durationSeconds;

	private String errorMessage;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	private Instant completedAt;

	protected CallRecord() {
	}

	/** An outbound call this app is placing. */
	public static CallRecord outbound(Long userId, String toNumber, String agentNumber, String twilioNumber) {
		CallRecord record = new CallRecord();
		record.userId = userId;
		record.direction = "OUTBOUND";
		record.toNumber = toNumber;
		record.agentNumber = agentNumber;
		record.twilioNumber = twilioNumber;
		return record;
	}

	/** Somebody rang our Twilio number. No agent owns it yet. */
	public static CallRecord inbound(String fromNumber, String twilioNumber, String callSid) {
		CallRecord record = new CallRecord();
		record.direction = "INBOUND";
		record.toNumber = fromNumber;
		record.twilioNumber = twilioNumber;
		record.callSid = callSid;
		record.status = "ringing";
		return record;
	}

	public boolean isInbound() {
		return "INBOUND".equals(direction);
	}

	/** What to show when the number is not in the address book. */
	public String displayName() {
		return contactName == null || contactName.isBlank() ? toNumber : contactName;
	}

	/** True once the call has reached a state it cannot leave. */
	public boolean isFinished() {
		return switch (status) {
			case "completed", "busy", "no-answer", "failed", "canceled" -> true;
			default -> false;
		};
	}

	public Long getId() { return id; }

	public Long getUserId() { return userId; }
	public void setUserId(Long userId) { this.userId = userId; }

	public String getDirection() { return direction; }

	public Long getContactId() { return contactId; }
	public void setContactId(Long contactId) { this.contactId = contactId; }

	public String getContactName() { return contactName; }
	public void setContactName(String contactName) { this.contactName = contactName; }

	public String getTwilioNumber() { return twilioNumber; }

	public Long getCallbackForCallId() { return callbackForCallId; }
	public void setCallbackForCallId(Long callbackForCallId) { this.callbackForCallId = callbackForCallId; }

	public String getToNumber() { return toNumber; }

	public String getAgentNumber() { return agentNumber; }
	public void setAgentNumber(String agentNumber) { this.agentNumber = agentNumber; }

	public String getCallSid() { return callSid; }
	public void setCallSid(String callSid) { this.callSid = callSid; }

	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }

	public Integer getDurationSeconds() { return durationSeconds; }
	public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

	public String getErrorMessage() { return errorMessage; }
	public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

	public Instant getCreatedAt() { return createdAt; }

	public Instant getCompletedAt() { return completedAt; }
	public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
