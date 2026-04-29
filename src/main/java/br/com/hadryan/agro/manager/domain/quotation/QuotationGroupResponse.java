package br.com.hadryan.agro.manager.domain.quotation;

import java.math.BigDecimal;
import java.util.List;

/**
 * Grupo de cotações do mesmo produto com métricas de economia calculadas.
 *
 * Economia = (preço máximo - preço mínimo) × quantidade da cotação mais barata
 * Representa quanto se economiza ao escolher o fornecedor mais barato
 * em vez do mais caro.
 */
public record QuotationGroupResponse(
        String productName,
        int quotationCount,
        BigDecimal minUnitPrice,
        BigDecimal maxUnitPrice,
        BigDecimal avgUnitPrice,
        // Economia potencial: (maxPrice - minPrice) × qty da cotação mais barata
        BigDecimal potentialSavings,
        // Fornecedor com o menor preço
        String cheapestSupplier,
        List<QuotationResponse> quotations
) {}