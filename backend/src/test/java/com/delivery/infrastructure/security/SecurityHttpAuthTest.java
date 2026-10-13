package com.delivery.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración — Seguridad JWT (HTTP 401 & HTTP 403)")
class SecurityHttpAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("Debe retornar HTTP 401 Unauthorized si se accede a ruta protegida sin token Bearer")
    void shouldReturn401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/v1/orders/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Debe retornar HTTP 403 Forbidden si usuario con rol CLIENT intenta acceder a rutas de ADMIN")
    void shouldReturn403WhenClientAttemptsAdminAccess() throws Exception {
        String clientToken = jwtTokenProvider.generateToken(
                "valeria.narvaez@tulcan.ec",
                UUID.randomUUID(),
                "CLIENT"
        );

        mockMvc.perform(get("/v1/admin/merchants")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Debe permitir acceso a endpoints públicos de autenticación (/v1/auth/quick-token)")
    void shouldAllowPublicAuthEndpoints() throws Exception {
        mockMvc.perform(post("/v1/auth/quick-token?role=CLIENT")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ROLE_CLIENT"));
    }
}
