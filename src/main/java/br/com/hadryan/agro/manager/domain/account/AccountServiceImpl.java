package br.com.hadryan.agro.manager.domain.account;

import br.com.hadryan.agro.manager.domain.user.User;
import br.com.hadryan.agro.manager.domain.user.UserRepository;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementação do serviço de gerenciamento de contas.
 */
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AccountResponse createAccount(UUID userId, CreateAccountRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", userId));

        Account account = Account.builder()
                .name(request.name())
                .owner(owner)
                .build();

        accountRepository.save(account);

        AccountMember ownerMembership = AccountMember.builder()
                .account(account)
                .user(owner)
                .role(AccountRole.OWNER)
                .build();

        accountMemberRepository.save(ownerMembership);

        return AccountResponse.from(account, AccountRole.OWNER, 1);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getUserAccounts(UUID userId) {
        List<AccountMember> memberships = accountMemberRepository.findByUserId(userId);

        if (memberships.isEmpty()) {
            return List.of();
        }

        // Coleta os IDs de todas as contas do usuário
        List<UUID> accountIds = memberships.stream()
                .map(m -> m.getAccount().getId())
                .toList();

        // Busca contagem de membros de todas as contas em uma única query (evita N+1)
        Map<UUID, Long> memberCountByAccount = accountMemberRepository
                .countMembersByAccountIds(accountIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));

        return memberships.stream()
                .map(member -> AccountResponse.from(
                        member.getAccount(),
                        member.getRole(),
                        memberCountByAccount.getOrDefault(member.getAccount().getId(), 1L)
                ))
                .toList();
    }

    @Override
    @Transactional
    public void deleteAccount(UUID accountId, UUID userId, DeleteAccountRequest request) {
        AccountMember caller = accountMemberRepository
                .findByAccountIdAndUserId(accountId, userId)
                .orElseThrow(() -> new BusinessException("Acesso negado", HttpStatus.FORBIDDEN));

        if (caller.getRole() != AccountRole.OWNER) {
            throw new BusinessException("Apenas o OWNER pode excluir a conta", HttpStatus.FORBIDDEN);
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));

        if (!account.getName().equals(request.confirmationName())) {
            throw new BusinessException(
                    "O nome digitado não confere com o nome da conta. Operação cancelada.",
                    HttpStatus.UNPROCESSABLE_CONTENT);
        }

        accountRepository.delete(account);
    }
}