package com.holaclimbing.server.infrastructure.ai;

import com.holaclimbing.server.domain.user.mapper.DeviceTokenMapper;
import com.holaclimbing.server.domain.video.domain.Video;
import com.holaclimbing.server.domain.video.mapper.VideoMapper;
import com.holaclimbing.server.infrastructure.fcm.FcmSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 분석 진행 이벤트 중 terminal 단계(COMPLETED/FAILED)에서 영상 소유자에게
 * FCM 푸시를 전송한다. SSE와 별도 채널이며, 사용자가 페이지를 떠나 있어도 도달한다.
 *
 * <p>FCM 미설정 환경에서는 NoopFcmSender가 주입되어 호출이 사실상 no-op이 된다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisCompletionNotifier {

    private final VideoMapper videoMapper;
    private final DeviceTokenMapper deviceTokenMapper;
    private final FcmSender fcmSender;

    /** terminal 단계가 아니면 즉시 반환. */
    public void notifyIfTerminal(AnalysisProgress progress) {
        AnalysisStage stage = progress.stage();
        if (stage != AnalysisStage.COMPLETED && stage != AnalysisStage.FAILED) {
            return;
        }
        try {
            Video video = videoMapper.findById(progress.videoId());
            if (video == null) {
                return;
            }
            List<String> tokens = deviceTokenMapper.findTokensByUserId(video.getUserId());
            if (tokens.isEmpty()) {
                return;
            }
            boolean success = stage == AnalysisStage.COMPLETED;
            String title = success ? "영상 분석 완료" : "영상 분석 실패";
            String body = success
                    ? "방금 업로드한 영상의 분석이 끝났어요. 확인해보세요."
                    : "영상 분석에 실패했어요. 잠시 후 다시 시도해보세요.";
            Map<String, String> data = Map.of(
                    "videoId", String.valueOf(video.getId()),
                    "stage", stage.name()
            );
            fcmSender.send(tokens, title, body, data);
        } catch (Exception e) {
            log.warn("분석 완료 FCM 전송 실패 — videoId={}: {}", progress.videoId(), e.getMessage());
        }
    }
}
