package br.com.hadryan.agro.manager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Contas — Gerenciamento de contas")
class AccountIntegrationTest extends MockMvcIntegrationTestBase {

    @Test
    @DisplayName("Deve criar uma nova conta e atribuir papel OWNER ao criador")
    void shouldCreateAccountAndAssignOwnerRole() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");

        mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Fazenda Boa Vista"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Fazenda Boa Vista"))
                .andExpect(jsonPath("$.data.userRole").value("OWNER"))
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @DisplayName("Deve listar todas as contas do usuário autenticado")
    void shouldListAllUserAccounts() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");
        createAccount(token, "Fazenda Alpha");
        createAccount(token, "Fazenda Beta");

        mockMvc.perform(get("/accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("Deve excluir conta com sucesso quando o nome de confirmação está correto")
    void shouldDeleteAccountSuccessfully() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");
        String accountId = createAccount(token, "Fazenda Temporária");

        mockMvc.perform(delete("/accounts/" + accountId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("confirmationName", "Fazenda Temporária"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verifica que a conta foi removida da lista do usuário
        mockMvc.perform(get("/accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("Deve rejeitar exclusão quando o nome de confirmação não corresponde")
    void shouldRejectDeleteWhenConfirmationNameDoesNotMatch() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");
        String accountId = createAccount(token, "Fazenda Real");

        mockMvc.perform(delete("/accounts/" + accountId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("confirmationName", "nome incorreto"))))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Deve rejeitar requisição sem token de autenticação")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        // Accept: application/json sinaliza ao authenticationEntryPoint que é uma
        // requisição de API — sem esse header o Spring Security redireciona (302) para
        // o OAuth2 em vez de retornar 401, pois trata como requisição de browser
        mockMvc.perform(get("/accounts")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve rejeitar criação de conta com nome muito curto")
    void shouldRejectAccountWithShortName() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");

        mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "A"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}