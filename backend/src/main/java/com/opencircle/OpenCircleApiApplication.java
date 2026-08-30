package com.opencircle;

import com.opencircle.location.LocationProperties;
import com.opencircle.mail.MailProperties;
import com.opencircle.passwordreset.PasswordResetProperties;
import com.opencircle.security.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.opencircle.security.JwtProperties;
import com.opencircle.verification.EmailVerificationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class, EmailVerificationProperties.class, MailProperties.class, PasswordResetProperties.class, LocationProperties.class})
public class OpenCircleApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenCircleApiApplication.class, args);
	}
}
