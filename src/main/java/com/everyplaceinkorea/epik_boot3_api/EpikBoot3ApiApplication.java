package com.everyplaceinkorea.epik_boot3_api;

import com.everyplaceinkorea.epik_boot3_api.config.CustomBeanNameGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(nameGenerator = CustomBeanNameGenerator.class)
public class EpikBoot3ApiApplication {

	// Claude Code Review 테스트를 위한 주석 추가
	// TODO: 성능 최적화 및 보안 강화 검토 필요
	public static void main(String[] args) {
		SpringApplication.run(EpikBoot3ApiApplication.class, args);
	}

}
