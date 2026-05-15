package br.com.hadryan.agro.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ClientOrderIntegrationTest extends MockMvcIntegrationTestBase {

    private String token;
    private String accountId;
    private String clientId;
    private String supplierId;
    private String ordersBase;

    @BeforeEach
    void setUp() throws Exception {
        String email = uniqueEmail();
        token = registerAndGetToken(email, "senha123");
        accountId = createAccount(token, "Conta Teste");
        ordersBase = "/accounts/" + accountId + "/trading/orders";

        clientId = createClient("Cliente Teste", "11999999999");
        supplierId = createSupplier("Fornecedor Teste");
    }

    private String createClient(String name, String phone) throws Exception {
        var payload = Map.of("name", name, "phone", phone);
        var result = mockMvc.perform(post("/accounts/" + accountId + "/trading/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    private String createSupplier(String name) throws Exception {
        var payload = Map.of("name", name, "phone", "11888888888");
        var result = mockMvc.perform(post("/accounts/" + accountId + "/trading/suppliers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    private Map<String, Object> orderPayload() {
        return Map.of(
                "clientId", clientId,
                "orderDate", "2026-05-14",
                "clientPricePerKg", 1.20,
                "legs", List.of(Map.of(
                        "supplierId", supplierId,
                        "supplierPricePerKg", 0.85,
                        "trucks", List.of(Map.of(
                                "truckPlate", "ABC1D23",
                                "quantityKg", 3000.0,
                                "freightValue", 150.0
                        ))
                ))
        );
    }

    private String createOrder() throws Exception {
        var result = mockMvc.perform(post(ordersBase)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderPayload())))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    @Test
    void createOrder_returnsCreatedWithLegsAndTrucks() throws Exception {
        mockMvc.perform(post(ordersBase)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderPayload())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.clientName").value("Cliente Teste"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.legs.length()").value(1))
                .andExpect(jsonPath("$.data.legs[0].trucks.length()").value(1))
                .andExpect(jsonPath("$.data.totalKg").value(3000.0))
                .andExpect(jsonPath("$.data.totalRevenue").value(3600.0))
                .andExpect(jsonPath("$.data.legs[0].trucks[0].freightValue").value(150.0));
    }

    @Test
    void createOrder_missingClientId_returns400() throws Exception {
        var payload = Map.of(
                "orderDate", "2026-05-14",
                "clientPricePerKg", 1.20,
                "legs", List.of()
        );
        mockMvc.perform(post(ordersBase)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listOrders_returnsPagedResults() throws Exception {
        createOrder();
        createOrder();

        mockMvc.perform(get(ordersBase).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    void listOrders_filterByStatus_returnsOnlyOpen() throws Exception {
        String orderId = createOrder();
        createOrder();

        mockMvc.perform(patch(ordersBase + "/" + orderId + "/close")
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(get(ordersBase + "?status=OPEN").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void getOrderDetail_returnsFullDetail() throws Exception {
        String orderId = createOrder();
        mockMvc.perform(get(ordersBase + "/" + orderId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.legs[0].supplierName").value("Fornecedor Teste"));
    }

    @Test
    void closeOrder_changesStatusToClosed() throws Exception {
        String orderId = createOrder();
        mockMvc.perform(patch(ordersBase + "/" + orderId + "/close")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ordersBase + "/" + orderId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    void deleteClosedOrder_returns409() throws Exception {
        String orderId = createOrder();
        mockMvc.perform(patch(ordersBase + "/" + orderId + "/close")
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(delete(ordersBase + "/" + orderId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteOpenOrder_returns204() throws Exception {
        String orderId = createOrder();
        mockMvc.perform(delete(ordersBase + "/" + orderId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void addLeg_toOpenOrder_returns201WithUpdatedDetail() throws Exception {
        String orderId = createOrder();
        String supplier2Id = createSupplier("Segundo Fornecedor");

        var legPayload = Map.of(
                "supplierId", supplier2Id,
                "supplierPricePerKg", 0.90,
                "trucks", List.of(Map.of("truckPlate", "XYZ9H87", "quantityKg", 2000.0))
        );

        mockMvc.perform(post(ordersBase + "/" + orderId + "/legs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(legPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.legs.length()").value(2))
                .andExpect(jsonPath("$.data.totalKg").value(5000.0));
    }

    @Test
    void removeLeg_fromOpenOrder_returns204() throws Exception {
        String orderId = createOrder();
        var detail = mockMvc.perform(get(ordersBase + "/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        String legId = objectMapper.readTree(detail.getResponse().getContentAsString())
                .path("data").path("legs").get(0).path("id").asText();

        mockMvc.perform(delete(ordersBase + "/" + orderId + "/legs/" + legId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteClient_withOrders_returns409() throws Exception {
        createOrder();
        mockMvc.perform(delete("/accounts/" + accountId + "/trading/clients/" + clientId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteSupplier_withLegs_returns409() throws Exception {
        createOrder();
        mockMvc.perform(delete("/accounts/" + accountId + "/trading/suppliers/" + supplierId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void getDashboard_returnsAggregatedMetrics() throws Exception {
        createOrder();
        mockMvc.perform(get("/accounts/" + accountId + "/trading/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(1))
                .andExpect(jsonPath("$.data.openOrders").value(1))
                .andExpect(jsonPath("$.data.totalKg").value(3000.0))
                .andExpect(jsonPath("$.data.totalClients").value(1))
                .andExpect(jsonPath("$.data.totalSuppliers").value(1));
    }
}
