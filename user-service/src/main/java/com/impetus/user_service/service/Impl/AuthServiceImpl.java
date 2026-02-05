package com.impetus.user_service.service.Impl;

import com.impetus.user_service.config.JwtTokenProvider;
import com.impetus.user_service.dto.auth.AuthResponse;
import com.impetus.user_service.dto.auth.LoginRequest;
import com.impetus.user_service.dto.auth.RefreshRequest;
import com.impetus.user_service.entity.Role;
import com.impetus.user_service.entity.User;
import com.impetus.user_service.repository.UserRepository;
import com.impetus.user_service.service.AuthService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final String kid = "auth-key-v1";

    @Override
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail().toLowerCase(Locale.ROOT)).orElseThrow(()-> new IllegalArgumentException("Invalid Credentials"));

        if(!user.getIsActive()){
            throw new SecurityException("User Disabled");
        }

        if(!passwordEncoder.matches(req.getPassword(), user.getPassword())){
            throw new IllegalArgumentException("Invalid credentials");
        }

        Set<String> roles = user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet());
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(user.getId()), roles, kid);
        String refreshToken = jwtTokenProvider.createRefreshToken(String.valueOf(user.getId()), kid);
        return new AuthResponse(accessToken, "Bearer",60*60*12, refreshToken);
    }

    @Override
    public AuthResponse refresh(RefreshRequest req) {
        try{
            var parsed = jwtTokenProvider.parseToken(req.getRefreshToken());
            Claims claims = parsed.getBody();
            Object typ = claims.get("typ");
            if(typ == null || !"refresh".equals(typ.toString())){
                throw new IllegalArgumentException("Invalid refresh token");
            }

            String subject = claims.getSubject();
            Long userId = Long.valueOf(subject);
            User user = userRepository.findById(userId).orElseThrow();
            List<String> roles = user.getRoles().stream().map(Role::getRoleName).toList();
            String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(user.getId()), roles, kid);
            return new AuthResponse(accessToken, "Bearer", 60*60*12, null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid refresh Token");
        }
    }

    @Override
    public void logout(RefreshRequest req) {

    }

    @Override
    public Boolean validate(String token) {
        return jwtTokenProvider.validate(token);
    }
}
