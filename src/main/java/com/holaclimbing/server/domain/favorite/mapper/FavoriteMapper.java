package com.holaclimbing.server.domain.favorite.mapper;

import com.holaclimbing.server.domain.gym.domain.Gym;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FavoriteMapper {

    void insert(@Param("userId") Long userId, @Param("gymId") Long gymId);

    int delete(@Param("userId") Long userId, @Param("gymId") Long gymId);

    boolean exists(@Param("userId") Long userId, @Param("gymId") Long gymId);

    /** 사용자의 즐겨찾기 개수 (활성 암장만 집계). */
    long countByUser(Long userId);

    /** 사용자가 즐겨찾기한 활성 암장 목록 (최신순). */
    List<Gym> findFavoriteGyms(@Param("userId") Long userId,
                               @Param("size") int size,
                               @Param("offset") int offset);
}
