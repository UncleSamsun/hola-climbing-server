package com.holaclimbing.server.domain.user.mapper;

import com.holaclimbing.server.domain.user.domain.DeviceToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeviceTokenMapper {

    /** 토큰 upsert. 같은 token이 있으면 user_id·platform·updated_at를 갱신. */
    void upsert(DeviceToken token);

    /** 본인 소유 토큰 삭제. 다른 사용자의 토큰은 영향받지 않는다. */
    int deleteByUserAndToken(@Param("userId") Long userId, @Param("token") String token);

    /** 사용자에 등록된 모든 토큰 문자열을 반환 (FCM 전송용). */
    List<String> findTokensByUserId(Long userId);

    /** 토큰 문자열로 단건 삭제 (FCM이 invalid 응답한 토큰 정리용). */
    int deleteByToken(String token);
}
