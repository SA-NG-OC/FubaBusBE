package com.example.Fuba_BE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing // Kích hoạt Auditing để @CreatedDate, @LastModifiedDate hoạt động (nếu dùng annotation chuẩn của Spring)
public class FubaBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(FubaBeApplication.class, args);
	}

}