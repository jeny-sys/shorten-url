package com.yourname.shorten;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShortenUrlApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShortenUrlApplication.class, args);
	}

}
