package com.demo.fix.initiator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class FixInitiatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(FixInitiatorApplication.class, args);
	}

}
