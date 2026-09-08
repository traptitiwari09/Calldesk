package com.example.App.call;

import com.twilio.twiml.TwiMLException;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Dial;
import com.twilio.twiml.voice.Hangup;
import com.twilio.twiml.voice.Number;
import com.twilio.twiml.voice.Say;


final class TwiML {

	private TwiML() {
	}

	/** Greets the person who answered, then dials the contact and bridges the two. */
	static String connect(String contactName, String contactNumber, String callerId) {
		Dial dial = new Dial.Builder()
				.callerId(callerId)
				.number(new Number.Builder(contactNumber).build())
				.timeout(30)
				.build();

		return toXml(new VoiceResponse.Builder()
				.say(new Say.Builder("Connecting you to " + contactName).build())
				.dial(dial)
				.build());
	}

	
	static String acknowledgeInbound() {
		return toXml(new VoiceResponse.Builder()
				.say(new Say.Builder(
						"Thanks for calling. Sorry we could not take your call right now. "
						+ "We have your number and one of our agents will call you back shortly. Goodbye.")
						.build())
				.hangup(new Hangup.Builder().build())
				.build());
	}

	/** Spoken when something is wrong and there is nobody to connect to. */
	static String saySomethingWentWrong(String message) {
		return toXml(new VoiceResponse.Builder()
				.say(new Say.Builder(message).build())
				.build());
	}

	private static String toXml(VoiceResponse response) {
		try {
			return response.toXml();
		} catch (TwiMLException ex) {
		
			throw new IllegalStateException("Could not build TwiML", ex);
		}
	}
}
