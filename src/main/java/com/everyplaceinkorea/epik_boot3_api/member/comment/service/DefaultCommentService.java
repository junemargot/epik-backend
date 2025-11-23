package com.everyplaceinkorea.epik_boot3_api.member.comment.service;

import com.everyplaceinkorea.epik_boot3_api.entity.comment.FeedComment;
import com.everyplaceinkorea.epik_boot3_api.entity.feed.Feed;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.member.comment.dto.CommentCreateDto;
import com.everyplaceinkorea.epik_boot3_api.member.comment.dto.CommentResponseDto;
import com.everyplaceinkorea.epik_boot3_api.member.comment.dto.CommentUpdateDto;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.comment.FeedCommentRepository;
import com.everyplaceinkorea.epik_boot3_api.repository.feed.FeedRepository;
import com.everyplaceinkorea.epik_boot3_api.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultCommentService implements CommentService{

    private final MemberRepository memberRepository;
    private final FeedRepository feedRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final ModelMapper modelMapper;

    @Transactional
    @Override
    public Long create(Long feedId, Long memberId, CommentCreateDto commentCreateDto) {
        Long currentMemberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findById(currentMemberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다."));

        FeedComment feedComment = FeedComment.builder()
                .content(commentCreateDto.getContent())
                .feed(feed)
                .member(member)
                .build();

        FeedComment savedComment = feedCommentRepository.save(feedComment);

        feedRepository.incrementCommentCount(feedId);

        return savedComment.getId();
    }

    // 피드 댓글 전체 조회
    @Override
    public List<CommentResponseDto> getList(Long feedId) {
        List<FeedComment> allByFeedId = feedCommentRepository.findAllByFeedId(feedId);
        List<CommentResponseDto> commentDtos = allByFeedId
                .stream()
                .map(comment -> {
                    CommentResponseDto commentDto = modelMapper.map(comment, CommentResponseDto.class);
                    commentDto.setId(comment.getId());
                    commentDto.setWriter(comment.getMember().getNickname());
                    commentDto.setContent(comment.getContent());
                    commentDto.setWriteDate(comment.getWriteDate());
                    return commentDto;
                }).toList();

        return commentDtos;
    }

    @Transactional
    @Override
    public void update(Long commentId, CommentUpdateDto updateDto) {
        Long currentMemberId = SecurityUtil.getCurrentMemberId();
        FeedComment feedComment = feedCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if(!feedComment.getMember().getId().equals(currentMemberId)) {
            throw new IllegalStateException("본인의 댓글만 수정할 수 있습니다.");
        }
        feedComment.update(updateDto);
    }

    @Transactional
    @Override
    public void delete(Long commentId) {
        Long currentMemberId = SecurityUtil.getCurrentMemberId();
        FeedComment feedComment = feedCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if(!feedComment.getMember().getId().equals(currentMemberId)) {
            throw new IllegalStateException("본인의 댓글만 삭제할 수 있습니다.");
        }

        Long feedId = feedComment.getFeed().getId();
        feedRepository.decrementCommentCount(feedId);

        feedComment.delete();
    }
}
