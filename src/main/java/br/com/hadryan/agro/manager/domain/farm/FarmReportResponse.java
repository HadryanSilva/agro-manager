package br.com.hadryan.agro.manager.domain.farm;

import br.com.hadryan.agro.manager.domain.expense.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Resposta completa do relatório financeiro de uma lavoura.
 * Todos os valores são calculados em memória a partir das despesas cadastradas.
 */
public record FarmReportResponse(

        // Dados identificadores da lavoura
        UUID farmId,
        String farmName,
        BigDecimal areaValue,
        AreaUnit areaUnit,
        FarmStatus status,

        // Dados do arrendamento (podem ser nulos)
        String lessorName,
        LocalDate leaseStartDate,
        LocalDate leaseEndDate,
        BigDecimal leaseValue,

        // Resumo financeiro das despesas
        BigDecimal totalExpenses,
        BigDecimal totalInsumos,
        BigDecimal totalServicos,
        BigDecimal totalPaid,
        BigDecimal totalPending,

        // Custo total = arrendamento + todas as despesas
        BigDecimal totalCost,

        // Evolução mensal ordenada cronologicamente
        List<MonthlyExpense> monthlyBreakdown,

        // Lista completa de despesas para detalhamento e exportação
        List<ExpenseItem> expenses

) {

    /**
     * Agregado mensal de despesas agrupadas por data de competência.
     */
    public record MonthlyExpense(
            int year,
            int month,
            // Ex: "Jan/2025"
            String monthLabel,
            BigDecimal total,
            BigDecimal paid,
            BigDecimal pending,
            BigDecimal insumos,
            BigDecimal servicos
    ) {}

    /**
     * Linha individual de despesa incluída no relatório.
     */
    public record ExpenseItem(
            UUID id,
            String description,
            ExpenseCategory category,
            BigDecimal value,
            LocalDate competenceDate,
            LocalDate paymentDate,
            boolean paid,
            String notes
    ) {}
}