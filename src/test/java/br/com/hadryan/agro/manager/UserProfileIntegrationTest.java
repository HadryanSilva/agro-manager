package br.com.hadryan.agro.manager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Perfil — Gestão de perfil do usuário")
class UserProfileIntegrationTest extends MockMvcIntegrationTestBase {

    @Test
    @DisplayName("Deve retornar perfil do usuário autenticado")
    void shouldReturnAuthenticatedUserProfile() throws Exception {
        String email = uniqueEmail();
        String token = registerAndGetToken(email, "senha12345");

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.authProvider").value("LOCAL"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @DisplayName("Deve atualizar o nome do usuário com sucesso")
    void shouldUpdateUserNameSuccessfully() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");

        mockMvc.perform(put("/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Nome Atualizado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Nome Atualizado"));
    }

    @Test
    @DisplayName("Deve alterar a senha com sucesso quando a senha atual está correta")
    void shouldChangePasswordSuccessfully() throws Exception {
        String email = uniqueEmail();
        String token = registerAndGetToken(email, "senha12345");

        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "senha12345",
                                "newPassword", "novaSenha789"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verifica que o login com a nova senha funciona
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "novaSenha789"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("Deve rejeitar alteração de senha quando a senha atual está incorreta")
    void shouldRejectPasswordChangeWhenCurrentPasswordIsWrong() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");

        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "senhaErrada",
                                "newPassword", "novaSenha789"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Deve rejeitar nova senha igual à senha atual")
    void shouldRejectNewPasswordSameAsCurrentPassword() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");

        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "senha12345",
                                "newPassword", "senha12345"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Deve rejeitar acesso ao perfil sem autenticação")
    void shouldRejectUnauthenticatedProfileAccess() throws Exception {
        // Accept: application/json sinaliza ao authenticationEntryPoint que é uma
        // requisição de API — sem esse header o Spring Security redireciona (302) para
        // o OAuth2 em vez de retornar 401, pois trata como requisição de browser
        mockMvc.perform(get("/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}