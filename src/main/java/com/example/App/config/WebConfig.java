package com.example.App.config;

import com.example.App.auth.AuthInterceptor;
import com.example.App.call.TwilioSignatureInterceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final AuthInterceptor authInterceptor;
	private final TwilioSignatureInterceptor signatureInterceptor;

	public WebConfig(AuthInterceptor authInterceptor, TwilioSignatureInterceptor signatureInterceptor) {
		this.authInterceptor = authInterceptor;
		this.signatureInterceptor = signatureInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(authInterceptor)
				.addPathPatterns("/api/**", "/dashboard.html", "/history.html")
				.excludePathPatterns(
						"/api/auth/login",
						"/api/auth/signup",
						"/api/auth/logout",
						"/api/auth/me");

		// Twilio's webhooks are proven genuine by their signature, not by a session.
		registry.addInterceptor(signatureInterceptor).addPathPatterns("/twilio/**");
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addRedirectViewController("/", "/dashboard.html");
	}
}
