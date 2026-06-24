package com.holaclimbing.server.domain.stats.dto;

import com.holaclimbing.server.common.exception.BusinessException;
import com.holaclimbing.server.common.exception.error.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;

public final class GymRankingCursorCodec {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final String SEPARATOR = ":";

    private GymRankingCursorCodec() {
    }

    public static String encode(GymRankingCursor cursor) {
        if (cursor == null
                || cursor.visitCount() <= 0
                || cursor.latestVisitDate() == null
                || cursor.gymId() == null
                || cursor.gymId() <= 0
                || cursor.rank() <= 0) {
            throw new IllegalArgumentException("암장 랭킹 커서 필드가 올바르지 않습니다.");
        }
        String payload = cursor.visitCount()
                + SEPARATOR + cursor.latestVisitDate()
                + SEPARATOR + cursor.gymId()
                + SEPARATOR + cursor.rank();
        return ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public static GymRankingCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String payload = new String(DECODER.decode(cursor), StandardCharsets.UTF_8);
            String[] parts = payload.split(SEPARATOR, -1);
            if (parts.length != 4) {
                throw new IllegalArgumentException("invalid cursor payload");
            }
            int visitCount = Integer.parseInt(parts[0]);
            LocalDate latestVisitDate = LocalDate.parse(parts[1]);
            long gymId = Long.parseLong(parts[2]);
            int rank = Integer.parseInt(parts[3]);
            if (visitCount <= 0 || gymId <= 0 || rank <= 0) {
                throw new IllegalArgumentException("invalid cursor field");
            }
            return new GymRankingCursor(visitCount, latestVisitDate, gymId, rank);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 커서입니다.");
        }
    }
}
