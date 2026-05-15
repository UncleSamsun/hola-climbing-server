package com.holaclimbing.server.domain.chat.service;

import com.holaclimbing.server.domain.chat.mapper.ChatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatMapper chatMapper;
    // TODO: Chat 서비스 구현
}
