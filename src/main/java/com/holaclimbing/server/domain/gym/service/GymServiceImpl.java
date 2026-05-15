package com.holaclimbing.server.domain.gym.service;

import com.holaclimbing.server.domain.gym.mapper.GymMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GymServiceImpl implements GymService {
    private final GymMapper gymMapper;
    // TODO: Gym 서비스 구현
}
