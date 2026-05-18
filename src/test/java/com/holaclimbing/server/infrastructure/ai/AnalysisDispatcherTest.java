package com.holaclimbing.server.infrastructure.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * AnalysisDispatcher 단위 테스트 — fire-and-forget 회복력 검증.
 */
class AnalysisDispatcherTest {

    @Test
    @DisplayName("ai.analysis-url 미설정 시 디스패치는 아무 동작 없이 통과한다")
    void dispatch_whenUrlBlank_isNoOp() {
        AnalysisDispatcher dispatcher = new AnalysisDispatcher("", "http://localhost:8080");

        assertThatNoException()
                .isThrownBy(() -> dispatcher.dispatch(1L, "videos/uploads/1/clip.mp4"));
    }

    @Test
    @DisplayName("워커에 연결할 수 없어도 예외를 삼키고 통과한다 (영상 등록에 영향 없음)")
    void dispatch_whenWorkerUnreachable_swallowsError() {
        AnalysisDispatcher dispatcher = new AnalysisDispatcher(
                "http://localhost:59999/analyze", "http://localhost:8080");

        assertThatNoException()
                .isThrownBy(() -> dispatcher.dispatch(1L, "videos/uploads/1/clip.mp4"));
    }
}
