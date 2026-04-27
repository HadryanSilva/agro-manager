package br.com.hadryan.agro.manager.domain.farm;

import br.com.hadryan.agro.manager.domain.account.Account;
import br.com.hadryan.agro.manager.domain.account.AccountMemberRepository;
import br.com.hadryan.agro.manager.domain.account.AccountRepository;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Serviço de gerenciamento de lavouras.
 * Todas as operações verificam que o usuário é membro da conta antes de prosseguir.
 */
@Service
@RequiredArgsConstructor
public class FarmService {

    private final FarmRepository farmRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;
    private final FarmActivityService activityService;

    @Transactional
    public FarmResponse create(UUID accountId, UUID userId, FarmRequest request) {
        Account account = findAccountAndValidateMembership(accountId, userId);

        Farm farm = Farm.builder()
                .account(account)
                .name(request.name())
                .areaValue(request.areaValue())
                .areaUnit(request.areaUnit())
                .lessorName(request.lessorName())
                .leaseStartDate(request.leaseStartDate())
                .leaseEndDate(request.leaseEndDate())
                .leaseValue(request.leaseValue())
                .plantingStartDate(request.plantingStartDate())
                .plantingEndDate(request.plantingEndDate())
                .harvestStartDate(request.harvestStartDate())
                .harvestEndDate(request.harvestEndDate())
                .cancelled(request.cancelled())
                .notes(request.notes())
                .build();

        return FarmResponse.from(farmRepository.save(farm));
    }

    @Transactional(readOnly = true)
    public List<FarmResponse> findAll(UUID accountId, UUID userId, FarmStatus statusFilter) {
        findAccountAndValidateMembership(accountId, userId);

        List<Farm> farms = farmRepository.findByAccountIdOrderByCreatedAtDesc(accountId);

        // Filtro por status aplicado em memória — volume de lavouras por conta é pequeno
        return farms.stream()
                .filter(f -> statusFilter == null || f.getStatus() == statusFilter)
                .map(FarmResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FarmResponse findById(UUID accountId, UUID userId, UUID farmId) {
        findAccountAndValidateMembership(accountId, userId);
        return FarmResponse.from(findFarm(farmId, accountId));
    }

    @Transactional
    public FarmResponse update(UUID accountId, UUID userId, UUID farmId, FarmRequest request) {
        findAccountAndValidateMembership(accountId, userId);
        Farm farm = findFarm(farmId, accountId);

        farm.setName(request.name());
        farm.setAreaValue(request.areaValue());
        farm.setAreaUnit(request.areaUnit());
        farm.setLessorName(request.lessorName());
        farm.setLeaseStartDate(request.leaseStartDate());
        farm.setLeaseEndDate(request.leaseEndDate());
        farm.setLeaseValue(request.leaseValue());
        farm.setPlantingStartDate(request.plantingStartDate());
        farm.setPlantingEndDate(request.plantingEndDate());
        farm.setHarvestStartDate(request.harvestStartDate());
        farm.setHarvestEndDate(request.harvestEndDate());
        farm.setCancelled(request.cancelled());
        farm.setNotes(request.notes());

        FarmResponse updatedFarm = FarmResponse.from(farmRepository.save(farm));
        activityService.record(
                farmId, userId,
                FarmActivityType.FARM_UPDATED,
                "Dados da lavoura atualizados",
                null
        );
        return updatedFarm;
    }

    @Transactional
    public void delete(UUID accountId, UUID userId, UUID farmId) {
        findAccountAndValidateMembership(accountId, userId);
        Farm farm = findFarm(farmId, accountId);
        farmRepository.delete(farm);
    }

    // ── Utilitários privados ──────────────────────────────────────────────────

    private Account findAccountAndValidateMembership(UUID accountId, UUID userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));

        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }

        return account;
    }

    private Farm findFarm(UUID farmId, UUID accountId) {
        return farmRepository.findByIdAndAccountId(farmId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Lavoura", "id", farmId));
    }
}