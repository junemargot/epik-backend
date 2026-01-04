package com.everyplaceinkorea.epik_boot3_api.member.musical.service;

import com.everyplaceinkorea.epik_boot3_api.entity.common.DataSource;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.Musical;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.MusicalBookmark;
import com.everyplaceinkorea.epik_boot3_api.entity.musical.MusicalBookmarkId;
import com.everyplaceinkorea.epik_boot3_api.image.service.ImageCacheService;
import com.everyplaceinkorea.epik_boot3_api.member.musical.dto.MusicalResponseDto;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalBookmarkRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.musical.MusicalRepository;
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
public class DefaultMusicalService implements MusicalService {

    private final MusicalRepository musicalRepository;
    private final MusicalBookmarkRepository musicalBookmarkRepository;
    private final MemberRepository memberRepository;
    private final ImageCacheService imageCacheService;

    @Override
    public List<MusicalResponseDto> getBookmark(Long id) {
        List<MusicalBookmark> bookmarks = musicalBookmarkRepository.findMusicalBookmarksByMemberId(id);

        return bookmarks.stream()
                .map(MusicalBookmark::getMusical)
                .map(musical -> {
                    String kopisPosterUrl = null;

                    if (musical.getDataSource() == DataSource.KOPIS_API && musical.getKopisPoster() != null) {
                        kopisPosterUrl = imageCacheService.getOrCacheImage(
                                musical.getKopisPoster(),
                                musical.getId().toString()
                        );
                    }
                    return MusicalResponseDto.builder()
                            .id(musical.getId())
                            .title(musical.getTitle())
                            .startDate(musical.getStartDate())
                            .endDate(musical.getEndDate())
                            .venue(musical.getVenue())
                            .saveImageName(musical.getFileSavedName())
                            .kopisPoster(kopisPosterUrl)
                            .build();
                })
                .collect(Collectors.toList());
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
                    .orElseThrow(() -> new EntityNotFoundException("Musical not found with id: " + musicalId));
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new EntityNotFoundException("Member not found with id: " + memberId));

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
