package com.example.deliveries;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DeliveriesApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeliveriesApplication.class, args);
	}

}
