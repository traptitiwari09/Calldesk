package com.example.App.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;


@Configuration
public class TwilioConfig {

	private static final Logger log = LoggerFactory.getLogger(TwilioConfig.class);

	private final TwilioProperties props;

	public TwilioConfig(TwilioProperties props) {
		this.props = props;
	}

	@PostConstruct
	void init() {
		if (!props.isConfigured()) {
			log.warn("Twilio is not configured - calling endpoints will return an error. "
					+ "Set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN and TWILIO_FROM_NUMBER.");
			return;
		}

		if (props.usesApiKey()) {
			// API key pair: revocable on its own, does not expose the master token.
			Twilio.init(props.getApiKeySid(), props.getApiKeySecret(), props.getAccountSid());
			log.info("Twilio initialised with API key {} on account {}",
					mask(props.getApiKeySid()), mask(props.getAccountSid()));
		} else {
			Twilio.init(props.getAccountSid(), props.getAuthToken());
			log.info("Twilio initialised with the account auth token on account {}", mask(props.getAccountSid()));
		}

		if (props.getPublicBaseUrl().isBlank()) {
			log.warn("twilio.public-base-url is empty. Twilio cannot reach localhost, so calls will not connect. "
					+ "Start ngrok and set APP_PUBLIC_BASE_URL to the https URL it prints.");
		}
		if (props.isValidateSignature() && props.getAuthToken().isBlank()) {
			log.warn("Webhook signature validation is on but no auth token is set - "
					+ "signature checks need the master auth token, not an API key. Webhooks will be rejected.");
		}
	}

	/** Show only the last 4 characters of a credential in logs. */
	private static String mask(String value) {
		if (value == null || value.length() <= 4) {
			return "****";
		}
		return "****" + value.substring(value.length() - 4);
	}
}
