package br.com.hadryan.agro.manager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Convites — Lookup por código curto")
class AccountInviteCodeIntegrationTest extends MockMvcIntegrationTestBase {

    @Test
    @DisplayName("Deve retornar detalhes do convite ao buscar pelo código curto")
    void shouldReturnInviteDetailsByCode() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail(), "senha12345");
        String accountId  = createAccount(ownerToken, "Fazenda Código");

        String inviteJson = mockMvc.perform(post("/accounts/" + accountId + "/invites")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", uniqueEmail(),
                                "role",  "MEMBER"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String formattedCode = objectMapper.readTree(inviteJson).path("data").path("code").asText();
        String rawCode = formattedCode.replace("-", "");

        mockMvc.perform(get("/invites/code/" + rawCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(formattedCode))
                .andExpect(jsonPath("$.data.accountName").value("Fazenda Código"));
    }

    @Test
    @DisplayName("Deve retornar 404 para código inexistente")
    void shouldReturn404ForUnknownCode() throws Exception {
        mockMvc.perform(get("/invites/code/INVALID12"))
                .andExpect(status().isNotFound());
    }
}
