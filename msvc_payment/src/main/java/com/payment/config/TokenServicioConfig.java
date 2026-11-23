package com.payment.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
public class TokenServicioConfig {

    private static final long MINUTOS_VALIDEZ = 5;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${spring.application.name}")
    private String nombreServicio;

    @Bean
    public RequestInterceptor tokenServicioInterceptor() {
        SecretKey clave = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(clave));
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();

        return template -> {
            Instant ahora = Instant.now();
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer(nombreServicio)
                    .subject(nombreServicio)
                    .issuedAt(ahora)
                    .expiresAt(ahora.plus(MINUTOS_VALIDEZ, ChronoUnit.MINUTES))
                    .claim("roles", List.of("ROLE_ADMIN"))
                    .build();
            String token = encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
            template.header("Authorization", "Bearer " + token);
        };
    }
}
