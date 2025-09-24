package com.everyplaceinkorea.epik_boot3_api.external.kopis.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JsonUtils {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static <T> String toJson(T object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      log.error("JSON 직렬화 실패: {}", e.getMessage());
      return "{}";
    }
  }

  public static <T> T fromJson(String json, Class<T> clazz) {
    if(json == null || json.trim().isEmpty()) {
      return null;
    }

    try {
      return objectMapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      log.error("JSON 역직렬화 실패: {}", e.getMessage());
      return null;
    }
  }

  public static Map<String, String> fromJsonToMap(String json) {
    if(json == null || json.trim().isEmpty()) {
      return new HashMap<>();
    }

    try {
      TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};
      return objectMapper.readValue(json, typeRef);
    } catch (JsonProcessingException e) {
      log.error("JSON Map 변환 실패: {}", e.getMessage());
      return new HashMap<>();
    }
  }

  /**
   * JSON 문자열이 유효한지 검증
   */
  public static boolean isValidJson(String json) {
    try {
      objectMapper.readTree(json);
      return true;
    } catch (JsonProcessingException e) {
      return false;
    }
  }
}
