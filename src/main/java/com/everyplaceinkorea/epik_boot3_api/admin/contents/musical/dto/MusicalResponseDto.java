package com.everyplaceinkorea.epik_boot3_api.admin.contents.musical.dto;

import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MusicalResponseDto {
    private Long id; // 등록된 뮤지컬의 식별 번호
    private String title; // 제목
    private String content; // 내용
    private String writer; // 작성자
    private String address; // 주소
    private String venue; // 장소
    private LocalDate startDate; // 시작일
    private LocalDate endDate; // 종료일
    private String saveImageName; // 저장된 이미지 경로
    private String imageUrl;      // 실제 이미지 URL (추가)
    private String runningTime; // 관람시간
    private String ageRestriction; // 관람 연령
    private List<MusicalTicketPriceDto> ticketPrices; // 티켓 금액
    private List<MusicalTicketOfficeDto> ticketOffices; // 티켓 예매처
    private LocalDateTime writeDate; // 등록일
    private DataSource dataSource;  // 데이터 출처

    private List<String> musicalImages; // KOPIS 상세 이미지들
    private String kopisPcseguidance;   // KOPIS 티켓 가격 정보

    // 이미지 URL 동적 생성
    public String getImageUrl() {
        if(dataSource == DataSource.KOPIS_API) {
            return this.imageUrl;
        } else {
            if(saveImageName != null && !saveImageName.trim().isEmpty()) {
                return "http://localhost:8081/api/v1/uploads/images/musical/" + saveImageName;
            }
        }
        return null;
    }

    // 테스트용 주석 추가
}