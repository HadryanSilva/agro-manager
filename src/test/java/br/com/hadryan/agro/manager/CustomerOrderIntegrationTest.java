package br.com.hadryan.agro.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CustomerOrderIntegrationTest extends MockMvcIntegrationTestBase {

    private String token;
    private String accountId;

    @BeforeEach
    void setUp() throws Exception {
        String email = uniqueEmail();
        token = registerAndGetToken(email, "senha123");
        accountId = createAccount(token, "Conta Teste");
    }

    @Test
    void createOrder_returnsCreatedWithPendingStatus() throws Exception {
        Map<String, Object> payload = orderPayload();

        mockMvc.perform(post("/accounts/" + accountId + "/trading/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.customerName").value("João Silva"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.product").value("Soja"));
    }

    @Test
    void listOrders_returnsAllWithDerivedStatus() throws Exception {
        createOrder(token, accountId);

        mockMvc.perform(get("/accounts/" + accountId + "/trading/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void listOrders_filterByStatus_returnOnlyMatching() throws Exception {
        createOrder(token, accountId);

        mockMvc.perform(get("/accounts/" + accountId + "/trading/orders?status=FULFILLED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/accounts/" + accountId + "/trading/orders?status=PENDING")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void getOrderById_returnsPendingOrder() throws Exception {
        String orderId = createOrder(token, accountId);

        mockMvc.perform(get("/accounts/" + accountId + "/trading/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void updateOrder_whenPending_returnsOk() throws Exception {
        String orderId = createOrder(token, accountId);
        Map<String, Object> updated = orderPayload();
        updated.put("customerName", "Maria Santos");

        mockMvc.perform(put("/accounts/" + accountId + "/trading/orders/" + orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerName").value("Maria Santos"));
    }

    @Test
    void deleteOrder_whenPending_returnsNoContent() throws Exception {
        String orderId = createOrder(token, accountId);

        mockMvc.perform(delete("/accounts/" + accountId + "/trading/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/accounts/" + accountId + "/trading/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrder_missingRequiredFields_returnsBadRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("customerName", "João");
        // Missing: quantityKg, product, orderDate

        mockMvc.perform(post("/accounts/" + accountId + "/trading/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> orderPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("customerName", "João Silva");
        payload.put("customerPhone", "11999999999");
        payload.put("customerDocument", "123.456.789-00");
        payload.put("quantityKg", 5000.0);
        payload.put("pricePerKg", 2.50);
        payload.put("product", "Soja");
        payload.put("orderDate", "2026-05-10");
        payload.put("deliveryDeadline", "2026-06-10");
        return payload;
    }

    protected String createOrder(String token, String accountId) throws Exception {
        var result = mockMvc.perform(post("/accounts/" + accountId + "/trading/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderPayload())))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }
}
