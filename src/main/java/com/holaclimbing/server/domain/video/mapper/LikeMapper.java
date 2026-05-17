package com.holaclimbing.server.domain.video.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LikeMapper {

    void insert(@Param("userId") Long userId, @Param("videoId") Long videoId);

    int delete(@Param("userId") Long userId, @Param("videoId") Long videoId);

    boolean exists(@Param("userId") Long userId, @Param("videoId") Long videoId);
}
