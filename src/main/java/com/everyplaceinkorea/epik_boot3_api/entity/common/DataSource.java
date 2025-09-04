package com.everyplaceinkorea.epik_boot3_api.entity.common;

public enum DataSource {
    MANUAL("수기 등록"),
    KOPIS_API("KOPIS API");
    
    private final String description;
    
    DataSource(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}