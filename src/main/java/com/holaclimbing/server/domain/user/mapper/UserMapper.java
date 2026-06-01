package com.holaclimbing.server.domain.user.mapper;

import com.holaclimbing.server.domain.user.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    /** 신규 회원 저장. 생성된 PK는 user.id로 채워진다. */
    void insert(User user);

    /** 활성 회원 단건 조회 (soft-delete 제외). 없으면 null. */
    User findById(Long id);

    /** 로그인용 이메일 조회 (soft-delete 제외). 없으면 null. */
    User findByEmail(String email);

    /** 이메일 인증 토큰으로 조회 (soft-delete 제외). 없으면 null. */
    User findByEmailVerificationToken(String token);

    /** 중복 검사용 — soft-delete된 행도 포함 (email UNIQUE 제약이 전체 행에 걸리므로). */
    boolean existsByEmail(String email);

    /** 중복 검사용 — soft-delete된 행도 포함 (nickname UNIQUE 제약이 전체 행에 걸리므로). */
    boolean existsByNickname(String nickname);

    /** 이메일 인증 완료 처리 — verified=true, 토큰 제거. */
    int markEmailVerified(Long id);

    /** 이메일 인증 토큰 재발급(갱신). */
    int updateEmailVerificationToken(@Param("id") Long id, @Param("token") String token);

    /** 마지막 로그인 시각 갱신. */
    int updateLastLoginAt(Long id);

    /** 프로필 부분 수정 — null이 아닌 필드만 갱신. */
    int updateProfile(@Param("id") Long id,
                      @Param("nickname") String nickname,
                      @Param("profileImage") String profileImage,
                      @Param("bio") String bio);

    /** 비밀번호 해시 갱신. */
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    /** 회원 탈퇴 — soft-delete (deleted_at 설정). */
    int softDelete(Long id);

    /**
     * 탈퇴 시 PII 익명화 — UNIQUE 제약을 풀어 동일 이메일/닉네임으로 재가입 가능하게 한다.
     * email = deleted_{id}@removed.local, nickname = deleted_{id}.
     */
    int anonymize(@Param("id") Long id,
                  @Param("email") String email,
                  @Param("nickname") String nickname);
}
