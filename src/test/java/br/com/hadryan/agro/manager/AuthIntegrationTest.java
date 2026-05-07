package br.com.hadryan.agro.manager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Auth — Fluxos de autenticação")
class AuthIntegrationTest extends MockMvcIntegrationTestBase {

    @Test
    @DisplayName("Deve registrar novo usuário com sucesso e retornar tokens JWT")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "João Silva");
        payload.put("email", uniqueEmail());
        payload.put("password", "senha12345");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").isNumber());
    }

    @Test
    @DisplayName("Deve realizar login com credenciais válidas")
    void shouldLoginWithValidCredentials() throws Exception {
        String email = uniqueEmail();
        String password = "senha12345";
        registerAndGetToken(email, password);

        Map<String, Object> payload = Map.of("email", email, "password", password);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("Deve renovar o access token usando refresh token válido")
    void shouldRefreshTokenSuccessfully() throws Exception {
        String email = uniqueEmail();

        Map<String, Object> registerPayload = new HashMap<>();
        registerPayload.put("name", "Usuário Refresh");
        registerPayload.put("email", email);
        registerPayload.put("password", "senha12345");

        // Registra e extrai o refresh token da resposta
        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isCreated())
                .andReturn();

        String refreshToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        // Usa o refresh token para obter novos tokens
        Map<String, Object> refreshPayload = Map.of("refreshToken", refreshToken);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("Deve rejeitar registro com email já cadastrado")
    void shouldRejectDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        registerAndGetToken(email, "senha12345");

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Outro Usuário");
        payload.put("email", email);
        payload.put("password", "senha12345");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("Deve rejeitar login com senha incorreta")
    void shouldRejectLoginWithWrongPassword() throws Exception {
        String email = uniqueEmail();
        registerAndGetToken(email, "senha12345");

        Map<String, Object> payload = Map.of("email", email, "password", "senhaErrada99");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Deve rejeitar registro com senha menor que 8 caracteres")
    void shouldRejectPasswordShorterThanEightChars() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Usuário");
        payload.put("email", uniqueEmail());
        payload.put("password", "123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Deve rejeitar refresh token inválido ou expirado")
    void shouldRejectInvalidRefreshToken() throws Exception {
        Map<String, Object> payload = Map.of("refreshToken", "token.invalido.aqui");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}