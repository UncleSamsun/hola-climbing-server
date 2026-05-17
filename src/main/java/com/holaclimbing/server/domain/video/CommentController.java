package com.holaclimbing.server.domain.video;

import com.holaclimbing.server.common.response.ApiResponse;
import com.holaclimbing.server.domain.video.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 댓글 삭제 API. 댓글 작성·조회는 VideoController(/api/videos/{id}/comments)에 있다.
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(@AuthenticationPrincipal Long userId,
                                           @PathVariable Long commentId) {
        commentService.deleteComment(userId, commentId);
        return ApiResponse.success();
    }
}
