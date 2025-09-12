package com.everyplaceinkorea.epik_boot3_api.anonymous.contents.musical.dto;

import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MusicalResponseDto {
    // 번호
    private Long id;
    // 대표이미지
    private String fileSavedName;
    // 제목
    private String title;
    // 장소
    private String venue;
    // 시작일
    private LocalDate startDate;
    // 종료일
    private LocalDate endDate;
    
    private String imageUrl;         // 실제 이미지 URL (추가)
    private DataSource dataSource;   // 데이터 출처

    // 이미지 URL을 동적으로 생성하는 메서드
    public String getImageUrl() {
        if(dataSource == DataSource.KOPIS_API) {
            // KOPIS 데이터인 경우 외부 URL 그대로 사용
            return this.imageUrl;
        } else {
            // 입력 데이터인 경우 로컬 서버 경로 사용
            if(fileSavedName != null && !fileSavedName.trim().isEmpty()) {
                return "http://localhost:8081/api/v1/uploads/images/musical/" + fileSavedName;
            }
        }
        return null; // 이미지가 없는 경우
    }
}
