package com.everyplaceinkorea.epik_boot3_api.member.popup.service;

import com.everyplaceinkorea.epik_boot3_api.entity.concert.Concert;
import com.everyplaceinkorea.epik_boot3_api.entity.concert.ConcertBookmark;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.entity.popup.Popup;
import com.everyplaceinkorea.epik_boot3_api.entity.popup.PopupBookmark;
import com.everyplaceinkorea.epik_boot3_api.entity.popup.PopupBookmarkId;
import com.everyplaceinkorea.epik_boot3_api.entity.popup.PopupImage;
import com.everyplaceinkorea.epik_boot3_api.member.concert.dto.ConcertResponseDto;
import com.everyplaceinkorea.epik_boot3_api.member.popup.dto.PopupResponseDto;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.ConcertBookmarkRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.concert.ConcertRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.popup.PopupBookmarkRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.popup.PopupImageRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.popup.PopupRepository;
import jakarta.persistence.EntityNotFoundException;
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
public class DefaultPopupService implements PopupService {

    private final PopupRepository popupRepository;
    private final PopupImageRepository popupImageRepository;
    private final PopupBookmarkRepository popupBookmarkRepository;
    private final MemberRepository memberRepository;

    @Override
    public List<PopupResponseDto> getBookmark(Long id) {
        List<PopupBookmark> bookmarks = popupBookmarkRepository.findPopupBookmarksByMemberId(id);
        List<Popup> popups = bookmarks.stream()
                .map(PopupBookmark::getPopup)
                .collect(Collectors.toList());

        List<PopupResponseDto> responseDtos = new ArrayList<>();

        popups.forEach(popup -> {
        Long popupId = popup.getId();
        Popup findPopup = popupRepository.findById(popupId).orElseThrow();
            List<PopupImage> popupImages = popupImageRepository.findAllByPopupId(findPopup.getId());
            String ImageSaveName = popupImages.get(0).getImgSavedName();
            PopupResponseDto responseDto = PopupResponseDto.builder()
                    .id(findPopup.getId())
                    .title(findPopup.getTitle())
                    .startDate(findPopup.getStartDate())
                    .endDate(findPopup.getEndDate())
                    .venue(findPopup.getAddress())
                    .saveImageName(ImageSaveName)
                    .build();

            responseDtos.add(responseDto);
        });

        return responseDtos;
    }

    @Override
    public boolean isBookmarked(Long popupId, Long memberId) {
        PopupBookmarkId id = new PopupBookmarkId();
        id.setPopupId(popupId);
        id.setMemberId(memberId);

        return popupBookmarkRepository.findById(id)
                .map(PopupBookmark::getIsActive)
                .orElse(false);
    }

    @Override
    public boolean toggleBookmark(Long popupId, Long memberId) {
        PopupBookmarkId id = new PopupBookmarkId();
        id.setPopupId(popupId);
        id.setMemberId(memberId);

        Optional<PopupBookmark> existingBookmark = popupBookmarkRepository.findById(id);

        if(existingBookmark.isPresent()) {
            PopupBookmark bookmark = existingBookmark.get();
            bookmark.setIsActive(!bookmark.getIsActive());
            popupBookmarkRepository.save(bookmark);
            return bookmark.getIsActive();
        } else {
            Popup popup = popupRepository.findById(popupId)
                    .orElseThrow(() -> new EntityNotFoundException("Popup not found"));
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            PopupBookmark newBookmark = new PopupBookmark();
            newBookmark.setId(id);
            newBookmark.setPopup(popup);
            newBookmark.setMember(member);
            newBookmark.setIsActive(true);

            popupBookmarkRepository.save(newBookmark);
            return true;
        }
    }


}
