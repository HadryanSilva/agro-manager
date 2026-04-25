package br.com.hadryan.agro.manager.domain.dashboard;

import br.com.hadryan.agro.manager.domain.account.AccountMemberRepository;
import br.com.hadryan.agro.manager.domain.account.AccountRepository;
import br.com.hadryan.agro.manager.domain.expense.ExpenseRepository;
import br.com.hadryan.agro.manager.domain.farm.AreaUnit;
import br.com.hadryan.agro.manager.domain.farm.Farm;
import br.com.hadryan.agro.manager.domain.farm.FarmRepository;
import br.com.hadryan.agro.manager.domain.farm.FarmStatus;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Serviço responsável por agregar as métricas exibidas no dashboard da conta.
 * A agregação de lavouras é feita em memória (volume pequeno por conta).
 * Os totais financeiros usam queries JPQL agregadas para evitar carregar todas as despesas.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FarmRepository farmRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public DashboardSummary getSummary(UUID accountId, UUID userId) {

        // Valida existência da conta e membership do usuário autenticado
        accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));

        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }

        // Carrega todas as lavouras da conta ordenadas pela mais recente
        List<Farm> farms = farmRepository.findByAccountIdOrderByCreatedAtDesc(accountId);

        // ── Contagens por status (calculado dinamicamente) ────────────────────
        long emPreparacao = count(farms, FarmStatus.EM_PREPARACAO);
        long emAndamento  = count(farms, FarmStatus.EM_ANDAMENTO);
        long colhida      = count(farms, FarmStatus.COLHIDA);
        long cancelada    = count(farms, FarmStatus.CANCELADA);

        // ── Área total separada por unidade ───────────────────────────────────
        BigDecimal totalHectares  = totalArea(farms, AreaUnit.HECTARE);
        BigDecimal totalAlqueires = totalArea(farms, AreaUnit.ALQUEIRE);

        // ── Arrendamentos vencendo nos próximos 30 dias ───────────────────────
        LocalDate hoje     = LocalDate.now();
        LocalDate em30Dias = hoje.plusDays(30);

        long leasesExpiring = farms.stream()
                .filter(f -> !f.isCancelled())
                .filter(f -> f.getLeaseEndDate() != null)
                .filter(f -> !f.getLeaseEndDate().isBefore(hoje)
                        && !f.getLeaseEndDate().isAfter(em30Dias))
                .count();

        // ── Totais financeiros da conta (queries agregadas — eficiente) ───────
        BigDecimal totalExpenses = expenseRepository.sumValueByAccountId(accountId);
        BigDecimal totalPaid     = expenseRepository.sumPaidValueByAccountId(accountId);
        BigDecimal totalPending  = totalExpenses.subtract(totalPaid);

        // ── Últimas 5 lavouras com seus totais de despesa individuais ─────────
        List<DashboardSummary.RecentFarm> recentFarms = farms.stream()
                .limit(5)
                .map(f -> {
                    BigDecimal farmTotal = expenseRepository.sumValueByFarmId(f.getId());
                    BigDecimal farmPaid  = expenseRepository.sumPaidValueByFarmId(f.getId());
                    return new DashboardSummary.RecentFarm(
                            f.getId(),
                            f.getName(),
                            f.getAreaValue(),
                            f.getAreaUnit(),
                            f.getStatus(),
                            f.getPlantingStartDate(),
                            f.getCreatedAt(),
                            farmTotal,
                            farmPaid
                    );
                })
                .toList();

        return new DashboardSummary(
                farms.size(),
                emPreparacao,
                emAndamento,
                colhida,
                cancelada,
                totalHectares,
                totalAlqueires,
                leasesExpiring,
                totalExpenses,
                totalPaid,
                totalPending,
                recentFarms
        );
    }

    // ── Utilitários privados ──────────────────────────────────────────────────

    private long count(List<Farm> farms, FarmStatus status) {
        return farms.stream().filter(f -> f.getStatus() == status).count();
    }

    private BigDecimal totalArea(List<Farm> farms, AreaUnit unit) {
        return farms.stream()
                .filter(f -> f.getAreaUnit() == unit)
                .map(Farm::getAreaValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}