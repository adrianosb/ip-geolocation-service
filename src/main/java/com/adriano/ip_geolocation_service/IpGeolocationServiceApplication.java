package com.adriano.ip_geolocation_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
public class IpGeolocationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IpGeolocationServiceApplication.class, args);
	}

}
