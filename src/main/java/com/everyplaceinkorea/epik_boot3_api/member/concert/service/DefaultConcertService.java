package com.everyplaceinkorea.epik_boot3_api.member.concert.service;

import com.everyplaceinkorea.epik_boot3_api.entity.concert.Concert;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.ConcertBookmark;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.ConcertBookmarkId;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.member.concert.dto.ConcertResponseDto;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.ConcertBookmarkRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.ConcertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultConcertService implements ConcertService {

    private final ConcertRepository concertRepository;
    private final ConcertBookmarkRepository concertBookmarkRepository;
    private final MemberRepository memberRepository;

    @Override
    public List<ConcertResponseDto> getBookmark(Long id) {
        List<ConcertBookmark> bookmarks = concertBookmarkRepository.findConcertBookmarksByMemberId(id);
        List<Concert> concerts = bookmarks.stream()
                .map(ConcertBookmark::getConcert)
                .collect(Collectors.toList());

        List<ConcertResponseDto> responseDtos = new ArrayList<>();

                concerts.forEach(concert -> {
            Long concertId = concert.getId();
            Concert findConcert = concertRepository.findById(concertId).orElseThrow();
            ConcertResponseDto responseDto = ConcertResponseDto.builder()
                    .id(findConcert.getId())
                    .title(findConcert.getTitle())
                    .startDate(findConcert.getStartDate())
                    .endDate(findConcert.getEndDate())
                    .venue(findConcert.getVenue())
                    .saveImageName(findConcert.getFileSavedName())
                    .build();

            responseDtos.add(responseDto);
        });

        return responseDtos;
    }

    // 북마크 상태 조회
    @Override
    public boolean isBookmarked(Long concertId, Long memberId) {
        ConcertBookmarkId id = new ConcertBookmarkId();
        id.setConcertId(concertId);
        id.setMemberId(memberId);

        return concertBookmarkRepository.findById(id)
                .map(ConcertBookmark::getIsActive)
                .orElse(false);
    }

    // 북마크 토글 (추가/삭제)
    @Override
    public boolean toggleBookmark(Long concertId, Long memberId) {
        ConcertBookmarkId id = new ConcertBookmarkId();
        id.setConcertId(concertId);
        id.setMemberId(memberId);

        Optional<ConcertBookmark> existingBookmark = concertBookmarkRepository.findById(id);

        if(existingBookmark.isPresent()) {
            // 이미 존재하면 isActive 상태 토글
            ConcertBookmark bookmark = existingBookmark.get();
            bookmark.setIsActive(!bookmark.getIsActive());
            concertBookmarkRepository.save(bookmark);
            return bookmark.getIsActive();
        } else {
            // 새로 생성
            Concert concert = concertRepository.findById(concertId)
                    .orElseThrow(() -> new RuntimeException("CONCERT NOT FOUND"));
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("MEMBER NOT FOUND"));

            ConcertBookmark newBookmark = new ConcertBookmark();
            newBookmark.setId(id);
            newBookmark.setConcert(concert);
            newBookmark.setMember(member);
            newBookmark.setIsActive(true);

            concertBookmarkRepository.save(newBookmark);
            return true;
        }
    }
}
