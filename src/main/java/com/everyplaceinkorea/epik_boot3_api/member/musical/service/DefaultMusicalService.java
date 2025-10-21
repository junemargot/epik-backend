package com.everyplaceinkorea.epik_boot3_api.member.musical.service;

import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.Musical;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.MusicalBookmark;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.MusicalBookmarkId;
import com.everyplaceinkorea.epik_boot3_api.member.musical.dto.MusicalResponseDto;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalBookmarkRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalRepository;
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
public class DefaultMusicalService implements MusicalService {

    private final MusicalRepository musicalRepository;
    private final MusicalBookmarkRepository musicalBookmarkRepository;
    private final MemberRepository memberRepository;

    @Override
    public List<MusicalResponseDto> getBookmark(Long id) {
        List<MusicalBookmark> bookmarks = musicalBookmarkRepository.findMusicalBookmarksByMemberId(id);
        List<Musical> musicals = bookmarks.stream()
                .map(MusicalBookmark::getMusical)
                .collect(Collectors.toList());

        List<MusicalResponseDto> responseDtos = new ArrayList<>();

                musicals.forEach(Musical -> {
            Long musicalId = Musical.getId();
            Musical findMusical = musicalRepository.findById(musicalId).orElseThrow();
            MusicalResponseDto responseDto = MusicalResponseDto.builder()
                    .id(findMusical.getId())
                    .title(findMusical.getTitle())
                    .startDate(findMusical.getStartDate())
                    .endDate(findMusical.getEndDate())
                    .venue(findMusical.getVenue())
                    .saveImageName(findMusical.getFileSavedName())
                    .build();

            responseDtos.add(responseDto);

        });

        return responseDtos;
    }

    @Override
    public boolean isBookmarked(Long musicalId, Long memberId) {
        MusicalBookmarkId id = new MusicalBookmarkId();
        id.setMusicalId(musicalId);
        id.setMemberId(memberId);

        return musicalBookmarkRepository.findById(id)
                .map(MusicalBookmark::getIsActive)
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean toggleBookmark(Long musicalId, Long memberId) {
        MusicalBookmarkId id = new MusicalBookmarkId();
        id.setMusicalId(musicalId);
        id.setMemberId(memberId);

        Optional<MusicalBookmark> existingBookmark = musicalBookmarkRepository.findById(id);

        if(existingBookmark.isPresent()) {
            MusicalBookmark bookmark = existingBookmark.get();
            bookmark.setIsActive(!bookmark.getIsActive());
            musicalBookmarkRepository.save(bookmark);
            return bookmark.getIsActive();
        } else {
            Musical musical = musicalRepository.findById(musicalId)
                    .orElseThrow(() -> new RuntimeException("Musical not found"));
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            MusicalBookmark newBookmark = new MusicalBookmark();
            newBookmark.setId(id);
            newBookmark.setMusical(musical);
            newBookmark.setMember(member);
            newBookmark.setIsActive(true);

            musicalBookmarkRepository.save(newBookmark);
            return true;
        }
    }


}
