package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;

public record TradingDashboardResponse(
        long totalOrders,
        long openOrders,
        long closedOrders,
        BigDecimal totalKg,
        BigDecimal totalRevenue,
        BigDecimal totalCost,
        BigDecimal grossMargin,
        long totalClients,
        long totalSuppliers
) {
}
