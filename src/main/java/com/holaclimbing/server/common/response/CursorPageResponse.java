package com.holaclimbing.server.common.response;

import java.util.List;

/**
 * 커서 기반 페이지 응답. 무한 스크롤 피드에 쓴다.
 *
 * <p>offset 페이지네이션과 달리 전체 개수(totalElements)·총 페이지 수를 계산하지 않는다.
 * 깊은 페이지에서도 일정한 성능(인덱스 keyset 스캔)을 보장하고, 새 글이 추가돼도
 * 중복·누락 없이 다음 페이지를 가져온다.</p>
 *
 * @param content    이번 페이지 항목
 * @param nextCursor 다음 페이지 요청 시 보낼 커서. 더 없으면 null.
 * @param hasNext    다음 페이지 존재 여부
 */
public record CursorPageResponse<T>(
        List<T> content,
        String nextCursor,
        boolean hasNext
) {
    public static <T> CursorPageResponse<T> of(List<T> content, String nextCursor, boolean hasNext) {
        return new CursorPageResponse<>(content, nextCursor, hasNext);
    }
}
