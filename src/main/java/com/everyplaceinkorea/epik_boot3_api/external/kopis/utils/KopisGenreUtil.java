package com.everyplaceinkorea.epik_boot3_api.external.kopis.utils;

import java.util.Arrays;

/**
 * KOPIS 장르 관리 유틸리티
 */
public class KopisGenreUtil {

  /**
   * 콘서트 장르 enum
   */
  public enum ConcertGenre {
    CLASSICAL("CCCA", "서양음악(클래식)"),
    POPULAR_MUSIC("CCCD", "대중음악"),
    DANCE_WESTERN_KOREAN("BBBC", "무용(서양/한국무용)"),
    POPULAR_DANCE("BBBE", "대중무용"),
    COMPLEX("EEEA", "복합");

    private final String code;
    private final String name;

    ConcertGenre(String code, String name) {
      this.code = code;
      this.name = name;
    }

    public String getCode() { return code;}
    public String getName() { return name;}

    public static String[] getAllCodes() {
      return Arrays.stream(values())
              .map(ConcertGenre::getCode)
              .toArray(String[]::new);
    }

    public static String getNameByCode(String code) {
      return Arrays.stream(values())
              .filter(genre -> genre.getCode().equals(code))
              .map(ConcertGenre::getName)
              .findFirst()
              .orElse("알 수 없는 공연 장르");
    }

    public static boolean isConcertCode(String code) {
      return Arrays.stream(values())
              .anyMatch(genre -> genre.getCode().equals(code));
    }

    public static boolean isConcertName(String name) {
      return Arrays.stream(values())
              .anyMatch(genre -> genre.getName().equals(name));
    }
  }

  /**
   * 뮤지컬 장르 enum
   */
  public enum MusicalGenre {
    MUSICAL("GGGA", "뮤지컬");

    private final String code;
    private final String name;

    MusicalGenre(String code, String name) {
      this.code = code;
      this.name = name;
    }

    public String getCode() { return code; }
    public String getName() { return name; }

    public static boolean isMusicalCode(String code) {
      return Arrays.stream(values())
              .anyMatch(genre -> genre.getCode().equals(code));
    }

    public static boolean isMusicalName(String name) {
      return Arrays.stream(values())
              .anyMatch(genre -> genre.getName().equals(name));
    }
  }

  /**
   * 유틸리티 메서드들
   */
  public static boolean isConcertGenre(String genreCodeOrName) {
    if (genreCodeOrName == null) return false;

    return ConcertGenre.isConcertCode(genreCodeOrName) ||
            ConcertGenre.isConcertName(genreCodeOrName);
  }

  public static boolean isMusicalGenre(String genreCodeOrName) {
    if (genreCodeOrName == null) return false;

    return MusicalGenre.isMusicalCode(genreCodeOrName) ||
            MusicalGenre.isMusicalName(genreCodeOrName);
  }

  public static String getConcertGenreName(String code) {
    return ConcertGenre.getNameByCode(code);
  }

  public static String getMusicalGenreName(String code) {
    return "GGGA".equals(code) ? "뮤지컬" : "알 수 없는 뮤지컬 장르";
  }
}
