package com.hktv.ars;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("com/hktv/ars/model")
public class ArsApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArsApplication.class, args);
	}

}
