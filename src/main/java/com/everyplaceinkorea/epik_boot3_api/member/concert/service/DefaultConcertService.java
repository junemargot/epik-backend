package com.everyplaceinkorea.epik_boot3_api.member.concert.service;

import com.everyplaceinkorea.epik_boot3_api.entity.concert.Concert;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.ConcertBookmark;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.ConcertBookmarkId;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.member.concert.dto.ConcertResponseDto;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.ConcertBookmarkRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.ConcertRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        return bookmarks.stream()
                .map(ConcertBookmark::getConcert)
                .map(concert -> ConcertResponseDto.builder()
                        .id(concert.getId())
                        .title(concert.getTitle())
                        .startDate(concert.getStartDate())
                        .endDate(concert.getEndDate())
                        .venue(concert.getVenue())
                        .saveImageName(concert.getFileSavedName())
                        .kopisPoster(concert.getKopisPoster())
                        .build())
                .collect(Collectors.toList());
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
    @Transactional
    public boolean toggleBookmark(Long concertId, Long memberId) {
        ConcertBookmarkId id = new ConcertBookmarkId();
        id.setConcertId(concertId);
        id.setMemberId(memberId);

        Optional<ConcertBookmark> existingBookmark = concertBookmarkRepository.findById(id);

        if (existingBookmark.isPresent()) {
            ConcertBookmark bookmark = existingBookmark.get();
            bookmark.setIsActive(!bookmark.getIsActive());
            concertBookmarkRepository.save(bookmark);
            return bookmark.getIsActive();
        } else {
            Concert concert = concertRepository.findById(concertId)
                    .orElseThrow(() -> new EntityNotFoundException("Concert not found with id: " + concertId));
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + memberId));

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
