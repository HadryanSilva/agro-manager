package br.com.hadryan.agro.manager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("UserPreferences — GET e PATCH /users/me/preferences")
class UserPreferencesIntegrationTest extends MockMvcIntegrationTestBase {

    @Test
    @DisplayName("Deve retornar preferências padrão (7 dias) para usuário novo")
    void shouldReturnDefaultPreferencesForNewUser() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");

        mockMvc.perform(get("/users/me/preferences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notificationDaysAhead").value(7));
    }

    @Test
    @DisplayName("Deve atualizar notificationDaysAhead para 14")
    void shouldUpdateNotificationDaysAhead() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");

        mockMvc.perform(patch("/users/me/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notificationDaysAhead", 14))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationDaysAhead").value(14));
    }

    @Test
    @DisplayName("Deve usar preferência do usuário no endpoint /upcoming quando days não informado")
    void shouldUseUserPreferenceInUpcomingWhenDaysNotProvided() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");
        String accountId = createAccount(token, "Fazenda Pref");
        String farmId = createFarm(token, accountId, "Lavoura Pref");

        // Atualiza preferência para 3 dias
        mockMvc.perform(patch("/users/me/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notificationDaysAhead", 3))))
                .andExpect(status().isOk());

        // Cria uma despesa vencendo em 2 dias (dentro da janela de 3)
        mockMvc.perform(post("/accounts/" + accountId + "/farms/" + farmId + "/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "Insumo dentro da janela",
                                "category", "INSUMO",
                                "value", 1000.0,
                                "competenceDate", "2025-04-01",
                                "creditPurchase", true,
                                "dueDate", java.time.LocalDate.now().plusDays(2).toString()
                        ))))
                .andExpect(status().isCreated());

        // Cria uma despesa vencendo em 5 dias (fora da janela de 3)
        mockMvc.perform(post("/accounts/" + accountId + "/farms/" + farmId + "/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "Insumo fora da janela",
                                "category", "INSUMO",
                                "value", 2000.0,
                                "competenceDate", "2025-04-01",
                                "creditPurchase", true,
                                "dueDate", java.time.LocalDate.now().plusDays(5).toString()
                        ))))
                .andExpect(status().isCreated());

        // /upcoming sem ?days deve usar a preferência do usuário (3 dias) — só retorna 1
        mockMvc.perform(get("/accounts/" + accountId + "/expenses/upcoming")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].description").value("Insumo dentro da janela"));
    }

    @Test
    @DisplayName("Deve rejeitar notificationDaysAhead = 0 — retorna 400")
    void shouldRejectZeroDaysAhead() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");

        mockMvc.perform(patch("/users/me/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notificationDaysAhead", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Deve rejeitar notificationDaysAhead = 91 — retorna 400")
    void shouldRejectDaysAheadAbove90() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");

        mockMvc.perform(patch("/users/me/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notificationDaysAhead", 91))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
