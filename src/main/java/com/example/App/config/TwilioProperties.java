package com.example.App.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "twilio")
public class TwilioProperties {

	
	private String accountSid ="";


	private String authToken ="";

	
	private String apiKeySid ="";

	private String apiKeySecret ="";

	
	private String fromNumber = "";

 
	private String publicBaseUrl = "";

	/** Verify the X-Twilio-Signature header on incoming webhooks. Keep true outside local experiments. */
	private boolean validateSignature = true;

	public boolean isConfigured() {
		return !accountSid.isBlank() && !fromNumber.isBlank()
				&& (!authToken.isBlank() || (!apiKeySid.isBlank() && !apiKeySecret.isBlank()));
	}

	/** True when we authenticate with an API key pair instead of the master auth token. */
	public boolean usesApiKey() {
		return !apiKeySid.isBlank() && !apiKeySecret.isBlank();
	}

	public String getAccountSid() { return accountSid; }
	public void setAccountSid(String accountSid) { this.accountSid = accountSid.trim(); }

	public String getAuthToken() { return authToken; }
	public void setAuthToken(String authToken) { this.authToken = authToken.trim(); }

	public String getApiKeySid() { return apiKeySid; }
	public void setApiKeySid(String apiKeySid) { this.apiKeySid = apiKeySid.trim(); }

	public String getApiKeySecret() { return apiKeySecret; }
	public void setApiKeySecret(String apiKeySecret) { this.apiKeySecret = apiKeySecret.trim(); }

	public String getFromNumber() { return fromNumber; }
	public void setFromNumber(String fromNumber) { this.fromNumber = fromNumber.trim(); }

	public String getPublicBaseUrl() { return publicBaseUrl; }
	public void setPublicBaseUrl(String publicBaseUrl) {
		String v = publicBaseUrl.trim();
		while (v.endsWith("/")) {
			v = v.substring(0, v.length() - 1);
		}
		this.publicBaseUrl = v;
	}

	public boolean isValidateSignature() { return validateSignature; }
	public void setValidateSignature(boolean validateSignature) { this.validateSignature = validateSignature; }
}
