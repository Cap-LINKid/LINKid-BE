package com.example.linkid.service;

import com.example.linkid.domain.Child;
import com.example.linkid.domain.User;
import com.example.linkid.dto.AuthDto;
import com.example.linkid.repository.ChildRepository;
import com.example.linkid.repository.UserRepository;
import com.example.linkid.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    // 1. 아이디 중복 확인
    @Transactional(readOnly = true)
    public boolean checkIdAvailability(String loginId) {
        return !userRepository.existsByLoginId(loginId);
    }

    // 2. 회원가입
    @Transactional
    public AuthDto.TokenResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByLoginId(request.getUser().getLoginId())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        // 유저 저장
        User user = User.builder()
                .loginId(request.getUser().getLoginId())
                .name(request.getUser().getName())
                .password(passwordEncoder.encode(request.getUser().getPassword()))
                .build();
        userRepository.save(user);

        // 자녀 저장
        Child child = Child.builder()
                .user(user)
                .name(request.getChild().getName())
                .birthdate(request.getChild().getBirthdate())
                .gender(request.getChild().getGender())
                .build();
        childRepository.save(child);

        // 회원가입 후 자동 로그인 처리 (토큰 발급)
        return generateToken(request.getUser().getLoginId(), request.getUser().getPassword());
    }

    // 3. 로그인
    @Transactional
    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        return generateToken(request.getLoginId(), request.getPassword());
    }

    // 토큰 생성 내부 로직
    private AuthDto.TokenResponse generateToken(String loginId, String password) {
        // 1. Login ID/PW 를 기반으로 Authentication 객체 생성
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginId, password);

        // 2. 실제 검증 (사용자 비밀번호 체크)
        // authenticate 매서드가 실행될 때 CustomUserDetailsService 에서 만든 loadUserByUsername 메서드가 실행됨
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        // Access Token: 1일, Refresh Token: 7일 (예시)
        String accessToken = jwtTokenProvider.createToken(authentication, 1000 * 60 * 60 * 24);
        String refreshToken = jwtTokenProvider.createToken(authentication, 1000 * 60 * 60 * 24 * 7);

        return new AuthDto.TokenResponse(accessToken, refreshToken);
    }
}