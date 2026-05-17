package com.holaclimbing.server.domain.video.service;

import com.holaclimbing.server.common.exception.BusinessException;
import com.holaclimbing.server.common.exception.error.ErrorCode;
import com.holaclimbing.server.common.response.PageResponse;
import com.holaclimbing.server.domain.gym.mapper.GymMapper;
import com.holaclimbing.server.domain.notification.service.NotificationService;
import com.holaclimbing.server.domain.video.domain.Video;
import com.holaclimbing.server.domain.video.dto.request.CreateVideoRequest;
import com.holaclimbing.server.domain.video.dto.request.UpdateVideoRequest;
import com.holaclimbing.server.domain.video.dto.response.VideoDetailResponse;
import com.holaclimbing.server.domain.video.dto.response.VideoSummaryResponse;
import com.holaclimbing.server.domain.video.mapper.LikeMapper;
import com.holaclimbing.server.domain.video.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoMapper videoMapper;
    private final LikeMapper likeMapper;
    private final GymMapper gymMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public VideoDetailResponse createVideo(Long userId, CreateVideoRequest request) {
        if (request.gymId() != null && gymMapper.findById(request.gymId()) == null) {
            throw new BusinessException(ErrorCode.GYM_NOT_FOUND);
        }
        Video video = Video.builder()
                .userId(userId)
                .gymId(request.gymId())
                .title(request.title())
                .description(request.description())
                .grade(request.grade())
                .gcsPath(request.gcsPath())
                .thumbnailPath(request.thumbnailPath())
                .durationSeconds(request.durationSeconds())
                .isPublic(request.isPublic() == null || request.isPublic())
                .build();
        videoMapper.insert(video);
        return VideoDetailResponse.of(videoMapper.findById(video.getId()), false);
    }

    @Override
    public PageResponse<VideoSummaryResponse> getFeed(Long uploaderId, int page, int size) {
        long total = videoMapper.countFeed(uploaderId);
        List<VideoSummaryResponse> content = videoMapper.findFeed(uploaderId, size, page * size)
                .stream().map(VideoSummaryResponse::from).toList();
        return PageResponse.of(content, page, size, total);
    }

    @Override
    @Transactional
    public VideoDetailResponse getVideoDetail(Long videoId, Long viewerId) {
        Video video = findActiveVideo(videoId);
        if (!video.isPublic() && !video.getUserId().equals(viewerId)) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_ACCESSIBLE);
        }
        videoMapper.incrementViewCount(videoId);
        boolean isLiked = viewerId != null && likeMapper.exists(viewerId, videoId);
        return VideoDetailResponse.of(videoMapper.findById(videoId), isLiked);
    }

    @Override
    @Transactional
    public VideoDetailResponse updateVideo(Long userId, Long videoId, UpdateVideoRequest request) {
        Video video = findActiveVideo(videoId);
        requireOwner(video, userId);
        videoMapper.updateVideo(videoId, request.title(), request.description(),
                request.grade(), request.isPublic());
        boolean isLiked = likeMapper.exists(userId, videoId);
        return VideoDetailResponse.of(videoMapper.findById(videoId), isLiked);
    }

    @Override
    @Transactional
    public void deleteVideo(Long userId, Long videoId) {
        Video video = findActiveVideo(videoId);
        requireOwner(video, userId);
        videoMapper.softDelete(videoId);
    }

    @Override
    @Transactional
    public void likeVideo(Long userId, Long videoId) {
        Video video = findActiveVideo(videoId);
        if (likeMapper.exists(userId, videoId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 좋아요한 영상입니다.");
        }
        likeMapper.insert(userId, videoId);
        videoMapper.incrementLikeCount(videoId);
        notificationService.notifyLike(video.getUserId(), userId, videoId);
    }

    @Override
    @Transactional
    public void unlikeVideo(Long userId, Long videoId) {
        if (likeMapper.delete(userId, videoId) > 0) {
            videoMapper.decrementLikeCount(videoId);
        }
    }

    private Video findActiveVideo(Long videoId) {
        Video video = videoMapper.findById(videoId);
        if (video == null) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND);
        }
        return video;
    }

    private void requireOwner(Video video, Long userId) {
        if (!video.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
