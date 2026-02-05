package com.impetus.api_gateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {
    private final JwtService jwtService;
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();
        if(!jwtService.isTokenValid(authToken)){
            log.info("Invalid Token");
            return Mono.empty();
        }

        String username = jwtService.extractUserName(authToken);
        log.info("Username: "+username);

        if(username == null){
            return Mono.empty();
        }

        List<SimpleGrantedAuthority> authorities = jwtService.extractRoles(authToken)
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        log.info("Authorities in JwtAReactiveAuth: "+authorities.toString());
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, authToken, authorities);
        return Mono.just(authenticationToken);
    }
}
