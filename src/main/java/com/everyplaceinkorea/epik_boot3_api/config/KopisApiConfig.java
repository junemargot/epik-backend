package com.everyplaceinkorea.epik_boot3_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kopis")
public class KopisApiConfig {
    
    private Api api = new Api();
    private Sync sync = new Sync();
    
    public static class Api {
        private String key;
        private String baseUrl;
        private int timeout;
        
        public String getKey() {
            return key;
        }
        
        public void setKey(String key) {
            this.key = key;
        }
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        public int getTimeout() {
            return timeout;
        }
        
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }
    
    public static class Sync {
        private boolean enabled;
        private String cron;
        private long detailDelayMs = 500;
        private long pageDelayMs = 500;
        private long migrationDelayMs = 500;
        private long retryDelayMs = 1000;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getCron() {
            return cron;
        }
        
        public void setCron(String cron) {
            this.cron = cron;
        }

        public long getDetailDelayMs() {
            return detailDelayMs;
        }
    
        public void setDetailDelayMs(long detailDelayMs) {
            this.detailDelayMs = detailDelayMs;
        }
    
        public long getPageDelayMs() {
            return pageDelayMs;
        }
    
        public void setPageDelayMs(long pageDelayMs) {
            this.pageDelayMs = pageDelayMs;
        }
    
        public long getMigrationDelayMs() {
            return migrationDelayMs;
        }
    
        public void setMigrationDelayMs(long migrationDelayMs) {
            this.migrationDelayMs = migrationDelayMs;
        }
    
        public long getRetryDelayMs() {
            return retryDelayMs;
        }
    
        public void setRetryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
        }
    }
    
    public Api getApi() {
        return api;
    }
    
    public void setApi(Api api) {
        this.api = api;
    }
    
    public Sync getSync() {
        return sync;
    }
    
    public void setSync(Sync sync) {
        this.sync = sync;
    }
}