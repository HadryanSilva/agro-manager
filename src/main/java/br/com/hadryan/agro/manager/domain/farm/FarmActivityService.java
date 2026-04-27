package br.com.hadryan.agro.manager.domain.farm;

import br.com.hadryan.agro.manager.domain.account.AccountMemberRepository;
import br.com.hadryan.agro.manager.domain.account.AccountRepository;
import br.com.hadryan.agro.manager.domain.user.User;
import br.com.hadryan.agro.manager.domain.user.UserRepository;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Serviço de histórico de atividades de uma lavoura.
 *
 * Métodos de registro automático (record*) usam REQUIRES_NEW para garantir
 * que a atividade seja persistida mesmo que a transação chamadora faça rollback
 * — isso evita perda de histórico por erros pontuais.
 */
@Service
@RequiredArgsConstructor
public class FarmActivityService {

    private final FarmActivityRepository activityRepository;
    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;

    // ── Leitura ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FarmActivityResponse> getActivities(UUID accountId, UUID farmId, UUID userId) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));

        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }

        farmRepository.findByIdAndAccountId(farmId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Lavoura", "id", farmId));

        return activityRepository.findByFarmIdOrderByCreatedAtDesc(farmId)
                .stream()
                .map(FarmActivityResponse::from)
                .toList();
    }

    // ── Anotação manual ───────────────────────────────────────────────────────

    @Transactional
    public FarmActivityResponse addNote(UUID accountId, UUID farmId, UUID userId, NoteRequest request) {
        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }

        Farm farm = farmRepository.findByIdAndAccountId(farmId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Lavoura", "id", farmId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", userId));

        FarmActivity activity = FarmActivity.builder()
                .farm(farm)
                .user(user)
                .type(FarmActivityType.NOTE)
                .description(request.description())
                .build();

        return FarmActivityResponse.from(activityRepository.save(activity));
    }

    // ── Registro automático (chamado pelos outros serviços) ───────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID farmId, UUID userId, FarmActivityType type, String description, UUID relatedId) {
        Farm farm = farmRepository.findById(farmId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);
        if (farm == null || user == null) return;

        FarmActivity activity = FarmActivity.builder()
                .farm(farm)
                .user(user)
                .type(type)
                .description(description)
                .relatedId(relatedId)
                .build();

        activityRepository.save(activity);
    }
}