package com.holaclimbing.server.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.ChannelTopic;

/**
 * 분석 진행 이벤트 구독을 위한 Redis MessageListenerContainer 설정.
 * 컨테이너는 Spring 시작 시 자동으로 구독을 개시하며, AnalysisProgressListener가 메시지를 받는다.
 */
@Configuration
@RequiredArgsConstructor
public class RedisAnalysisConfig {

    @Bean
    public RedisMessageListenerContainer analysisProgressListenerContainer(
            RedisConnectionFactory connectionFactory,
            AnalysisProgressListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listener, new ChannelTopic(RedisAnalysisProgressBus.CHANNEL));
        return container;
    }
}
