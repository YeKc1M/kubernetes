package com.example.httpdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
		"controller"
})
public class HttpdemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(HttpdemoApplication.class, args);
	}

}
