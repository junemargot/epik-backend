package com.everyplaceinkorea.epik_boot3_api.external.kopis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "scraping")
public class ScrapingProperties {
  private boolean enabled = true;
  private int delayBetweenRequests = 3000;
  private int timeoutSeconds = 15;
  private boolean headlessMode = true;
  private int maxRetryCount = 3;
  private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
}
