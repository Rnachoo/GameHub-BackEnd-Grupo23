package com.auth.config;

import com.auth.security.JwtService;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    // Agrega el token de servicio a cada llamada Feign saliente (hoy solo va a msvc-user)
    @Bean
    public RequestInterceptor tokenDeServicio(JwtService jwtService) {
        return plantilla -> plantilla.header("Authorization", "Bearer " + jwtService.generarTokenServicio());
    }
}