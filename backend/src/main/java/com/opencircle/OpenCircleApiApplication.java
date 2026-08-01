package com.opencircle;

import com.opencircle.security.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.opencircle.security.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class OpenCircleApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenCircleApiApplication.class, args);
	}
}
