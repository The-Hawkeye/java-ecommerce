package com.impetus.api_gateway.config;

import com.impetus.api_gateway.security.JwtSecurityContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtSecurityContextRepository jwtSecurityContextRepository;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http){
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .securityContextRepository(jwtSecurityContextRepository)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(authorizeExchangeSpec ->
                        authorizeExchangeSpec.pathMatchers("/auth/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/user/register").permitAll()
                                .pathMatchers(HttpMethod.GET, "/user/all").hasAuthority("ADMIN")
                                .pathMatchers(HttpMethod.GET, "/product/**").permitAll()
                                .pathMatchers(HttpMethod.POST, "/product/detailsOfIds").hasAnyAuthority("USER", "ADMIN")
                                .pathMatchers(HttpMethod.POST, "/product/updateInventory").hasAnyAuthority("USER", "ADMIN")
                                .pathMatchers(HttpMethod.POST, "/product/**").hasAuthority("ADMIN")
                                .pathMatchers("/order/listAllOrders/**").hasAuthority("ADMIN")
                                .pathMatchers("/order/listOrdersOfUser/**").hasAuthority("ADMIN")
                                .pathMatchers("/order/**").hasAuthority("USER")
                                .pathMatchers("/cart/**").hasAuthority("USER")
                                .pathMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**","/swagger-ui.html", "/webjars/**","/swagger-ui/webjars/swagger-ui/index.html", "/eureka/**", "/proxy", "/proxy/**").permitAll()
                                .anyExchange().authenticated())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
