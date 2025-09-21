package com.everyplaceinkorea.epik_boot3_api;

import com.everyplaceinkorea.epik_boot3_api.config.CustomBeanNameGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(nameGenerator = CustomBeanNameGenerator.class)
public class EpikBoot3ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EpikBoot3ApiApplication.class, args);
	}

}
