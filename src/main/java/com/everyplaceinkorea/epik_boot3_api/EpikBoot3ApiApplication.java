package com.everyplaceinkorea.epik_boot3_api;

import com.everyplaceinkorea.epik_boot3_api.config.CustomBeanNameGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@ComponentScan(nameGenerator = CustomBeanNameGenerator.class)
@EnableJpaAuditing
public class EpikBoot3ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EpikBoot3ApiApplication.class, args);
	}

}
