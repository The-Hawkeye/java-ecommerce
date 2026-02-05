package com.impetus.user_service.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if(!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer")){
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        logger.info(token+"Received Token");
        try{
            logger.info("Before parsing token");
            var jws = jwtTokenProvider.parseToken(token);
            logger.info("after parsing");
            logger.info(jws.toString());
            Claims claims = jws.getBody();

            String subject = claims.getSubject();

            System.out.println(subject.toString()+ "This is subject");

            Object rolesObj = claims.get("roles");
            List<String> roles = List.of();
            if(rolesObj instanceof Collection<?> c){
                roles = c.stream().map(Object::toString).toList();
                System.out.println(roles.size()+"Size of roles");
            }

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role->new SimpleGrantedAuthority("ROLE_"+role))
                    .toList();

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(subject, null, authorities);
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        } catch (Exception e) {
            logger.info("Error Occurred while parsing token");
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
