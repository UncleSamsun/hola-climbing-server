package com.holaclimbing.server.domain.video.dto.response;

/**
 * 영상 공유 링크 응답. 클라이언트는 이 URL을 그대로 클립보드·SNS 공유에 사용한다.
 */
public record ShareLinkResponse(
        String shareUrl
) {
}
