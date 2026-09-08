package com.example.App.common;

/**
 * Normalises whatever a user types into E.164 ("+15551234567").
 *
 * Twilio rejects anything else, and a malformed number is the single most common
 * reason a first call fails, so every number is pushed through here before it is stored.
 */
public final class PhoneNumbers {

	private PhoneNumbers() {
	}

	/**
	 * @param raw                what the user typed, e.g. "(555) 123-4567" or "+1 555 123 4567"
	 * @param defaultCountryCode dial code (with "+") applied when the input has no country code
	 * @return the number in E.164
	 * @throws ApiException if the input cannot be read as a phone number
	 */
	public static String toE164(String raw, String defaultCountryCode) {
		if (raw == null || raw.isBlank()) {
			throw new ApiException("Phone number is required");
		}

		String trimmed = raw.trim();
		boolean explicitPlus = trimmed.startsWith("+");
		// Some people paste "0011..." - the international prefix means the same as "+".
		if (!explicitPlus && trimmed.startsWith("00")) {
			explicitPlus = true;
			trimmed = trimmed.substring(2);
		}

		String digits = trimmed.replaceAll("[^0-9]", "");
		if (digits.isEmpty()) {
			throw new ApiException("'" + raw + "' is not a phone number");
		}

		String e164 = explicitPlus ? "+" + digits : applyDefaultCountry(digits, defaultCountryCode);

		// E.164 allows at most 15 digits; anything under 8 is not a reachable number.
		int length = e164.length() - 1;
		if (length < 8 || length > 15) {
			throw new ApiException("'" + raw + "' does not look like a valid phone number. "
					+ "Include the country code, for example +15551234567");
		}
		return e164;
	}

	private static String applyDefaultCountry(String digits, String defaultCountryCode) {
		String cc = defaultCountryCode.startsWith("+") ? defaultCountryCode.substring(1) : defaultCountryCode;

		// Already carries the country code, just without the "+".
		if (digits.startsWith(cc) && digits.length() > cc.length() + 6) {
			return "+" + digits;
		}
		// A national number with a trunk "0" in front, as written in India and much of Europe.
		if (digits.startsWith("0")) {
			return "+" + cc + digits.substring(1);
		}
		return "+" + cc + digits;
	}

	/** Formats for display only - never send this back to Twilio. */
	public static String forDisplay(String e164) {
		if (e164 != null && e164.startsWith("+1") && e164.length() == 12) {
			return "+1 " + e164.substring(2, 5) + " " + e164.substring(5, 8) + " " + e164.substring(8);
		}
		return e164;
	}
}
