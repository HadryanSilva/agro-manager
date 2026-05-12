package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;

/**
 * Resposta do dashboard do modo atravessador.
 * Métricas agregadas da conta no período ou total histórico.
 */
public record TradingDashboardResponse(
        long totalLots,
        long openLots,
        long closedLots,
        BigDecimal totalPurchasedKg,
        BigDecimal totalSoldKg,
        BigDecimal totalCost,
        BigDecimal totalRevenue,
        BigDecimal grossMargin,
        long totalSuppliers
) {}
