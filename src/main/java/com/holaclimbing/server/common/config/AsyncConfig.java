package com.holaclimbing.server.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @Async 활성화. AI 분석 디스패치 등 fire-and-forget 비동기 작업에 사용한다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
