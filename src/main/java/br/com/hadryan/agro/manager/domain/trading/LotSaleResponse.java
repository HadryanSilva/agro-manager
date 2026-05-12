package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resposta de uma venda de lote com caminhões e totais calculados.
 */
public record LotSaleResponse(
        UUID id,
        String buyerName,
        LocalDate saleDate,
        BigDecimal pricePerKg,
        List<LotSaleTruckResponse> trucks,
        BigDecimal totalKg,
        BigDecimal totalRevenue,
        String notes,
        LocalDateTime createdAt
) {
    public static LotSaleResponse from(LotSale sale) {
        List<LotSaleTruckResponse> truckResponses = sale.getTrucks().stream()
                .map(LotSaleTruckResponse::from)
                .toList();

        BigDecimal totalKg = truckResponses.stream()
                .map(LotSaleTruckResponse::quantityKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = totalKg.multiply(sale.getPricePerKg());

        return new LotSaleResponse(
                sale.getId(), sale.getBuyerName(), sale.getSaleDate(),
                sale.getPricePerKg(), truckResponses, totalKg, totalRevenue,
                sale.getNotes(), sale.getCreatedAt()
        );
    }
}
