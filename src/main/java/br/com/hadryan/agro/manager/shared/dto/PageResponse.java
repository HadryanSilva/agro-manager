package br.com.hadryan.agro.manager.shared.dto;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

/**
 * Wrapper genérico para respostas paginadas.
 * Inclui campo totalValue para exibição de totalizadores financeiros no frontend.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last,
        BigDecimal totalValue
) {
    public static <T> PageResponse<T> of(Page<T> page, BigDecimal totalValue) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                totalValue
        );
    }
}