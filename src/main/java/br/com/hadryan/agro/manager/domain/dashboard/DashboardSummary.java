package br.com.hadryan.agro.manager.domain.dashboard;

import br.com.hadryan.agro.manager.domain.farm.AreaUnit;
import br.com.hadryan.agro.manager.domain.farm.FarmStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resposta agregada do dashboard de uma conta.
 * Contém métricas calculadas em memória a partir das lavouras e despesas da conta.
 */
public record DashboardSummary(

        // Contagem total e por status
        long totalFarms,
        long emPreparacao,
        long emAndamento,
        long colhida,
        long cancelada,

        // Área total separada por unidade (sem conversão para não perder precisão)
        BigDecimal totalAreaHectares,
        BigDecimal totalAreaAlqueires,

        // Arrendamentos vencendo nos próximos 30 dias (apenas lavouras ativas)
        long leasesExpiringIn30Days,

        // Resumo financeiro consolidado da conta
        BigDecimal totalExpenses,
        BigDecimal totalExpensesPaid,
        BigDecimal totalExpensesPending,

        // Últimas 5 lavouras cadastradas (com totais de despesa individuais)
        List<RecentFarm> recentFarms

) {

    /**
     * Projeção leve de lavoura usada na lista de atividade recente.
     * Inclui totais de despesa para exibição rápida sem chamadas adicionais.
     */
    public record RecentFarm(
            UUID id,
            String name,
            BigDecimal areaValue,
            AreaUnit areaUnit,
            FarmStatus status,
            LocalDate plantingStartDate,
            LocalDateTime createdAt,
            BigDecimal totalExpenses,
            BigDecimal totalExpensesPaid
    ) {}
}