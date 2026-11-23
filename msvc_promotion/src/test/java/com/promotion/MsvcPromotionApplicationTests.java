package com.promotion;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MsvcPromotionApplicationTests {

    @LocalServerPort
    private int port;

    @Value("${jwt.secret}")
    private String secret;

    @Test
    void aplicarCuponRequiereLaAutoridadDelServicioDeOrdenes() throws Exception {
        URI url = URI.create("http://localhost:" + port
                + "/api/v1/promociones/CUPON-INEXISTENTE-TEST/aplicar?totalOrden=10000");
        try (HttpClient client = HttpClient.newHttpClient()) {
            assertEquals(401, aplicarCupon(client, url, null));
            assertEquals(403, aplicarCupon(client, url, token("msvc-auth", "ROLE_USER")));
            assertEquals(403, aplicarCupon(client, url, token("msvc-auth", "ROLE_ADMIN")));
            // Un token autorizado llega al servicio y recibe el error de cupón inexistente.
            assertEquals(404, aplicarCupon(client, url, token("msvc-order", "ROLE_ADMIN", "APLICAR_CUPONES")));
        }
    }

    private int aplicarCupon(HttpClient client, URI url, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private String token(String issuer, String... authorities) {
        var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var headers = JwsHeader.with(MacAlgorithm.HS256).build();
        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("prueba-promociones")
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("roles", List.of(authorities))
                .build();
        return encoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }
}
