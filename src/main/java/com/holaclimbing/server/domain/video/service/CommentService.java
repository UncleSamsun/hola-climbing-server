package com.holaclimbing.server.domain.video.service;

import com.holaclimbing.server.common.response.PageResponse;
import com.holaclimbing.server.domain.video.dto.request.CreateCommentRequest;
import com.holaclimbing.server.domain.video.dto.response.CommentResponse;

public interface CommentService {

    /** 영상에 댓글 작성. parentId가 있으면 대댓글. */
    CommentResponse addComment(Long userId, Long videoId, CreateCommentRequest request);

    /** 영상의 댓글 목록 조회. */
    PageResponse<CommentResponse> getComments(Long videoId, int page, int size);

    /** 댓글 삭제 (작성자만, soft-delete). */
    void deleteComment(Long userId, Long commentId);
}
