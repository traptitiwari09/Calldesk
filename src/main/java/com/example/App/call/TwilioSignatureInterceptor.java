package com.example.App.call;

import java.util.HashMap;
import java.util.Map;

import com.example.App.config.TwilioProperties;
import com.twilio.security.RequestValidator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Proves an incoming /twilio/** request really came from Twilio.
 *
 * The webhook URLs are public - anyone who learns your ngrok address could otherwise
 * POST to /twilio/voice and make your app dial a number. Twilio signs every request
 * with your auth token; this recomputes that signature and rejects anything that differs.
 */
@Component
public class TwilioSignatureInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(TwilioSignatureInterceptor.class);

	private final TwilioProperties props;

	public TwilioSignatureInterceptor(TwilioProperties props) {
		this.props = props;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		if (!props.isValidateSignature()) {
			return true;
		}

		if (props.getAuthToken().isBlank()) {
			// Signatures are computed from the master auth token; an API key cannot verify them.
			log.error("Rejecting Twilio webhook: signature validation is on but no auth token is configured. "
					+ "Set TWILIO_AUTH_TOKEN, or set twilio.validate-signature=false for local testing.");
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}

		String signature = request.getHeader("X-Twilio-Signature");
		if (signature == null) {
			log.warn("Rejecting {} {} - no X-Twilio-Signature header", request.getMethod(), request.getRequestURI());
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}

		if (!new RequestValidator(props.getAuthToken()).validate(publicUrlOf(request), formParams(request), signature)) {
			log.warn("Rejecting {} {} - signature did not match. "
					+ "Usually this means twilio.public-base-url does not match the URL Twilio actually called.",
					request.getMethod(), request.getRequestURI());
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}

		return true;
	}

	/**
	 * Twilio signed the public URL it dialled, not the localhost URL ngrok forwarded to us,
	 * so the signature only matches if we rebuild the public one.
	 */
	private String publicUrlOf(HttpServletRequest request) {
		String base = props.getPublicBaseUrl();
		if (base.isBlank()) {
			base = request.getRequestURL().substring(0,
					request.getRequestURL().length() - request.getRequestURI().length());
		}
		String query = request.getQueryString();
		return base + request.getRequestURI() + (query == null ? "" : "?" + query);
	}

	/** Twilio posts webhooks as form data; the signature covers those fields. */
	private static Map<String, String> formParams(HttpServletRequest request) {
		Map<String, String> params = new HashMap<>();
		if ("POST".equalsIgnoreCase(request.getMethod())) {
			request.getParameterMap().forEach((key, values) -> {
				if (values.length > 0) {
					params.put(key, values[0]);
				}
			});
		}
		return params;
	}
}
