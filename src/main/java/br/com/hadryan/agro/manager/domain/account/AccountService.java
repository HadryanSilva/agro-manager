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
import java.util.UUID;

/**
 * Serviço responsável pelo gerenciamento de contas.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;
    private final UserRepository userRepository;

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

    @Transactional(readOnly = true)
    public List<AccountResponse> getUserAccounts(UUID userId) {
        return accountMemberRepository.findByUserId(userId).stream()
                .map(member -> {
                    long memberCount = accountMemberRepository.countByAccountId(
                            member.getAccount().getId()
                    );
                    return AccountResponse.from(member.getAccount(), member.getRole(), memberCount);
                })
                .toList();
    }

    /**
     * Exclui permanentemente uma conta e todos os seus dados.
     * <br>
     * Camadas de segurança:
     *   1. Somente o OWNER pode excluir a conta
     *   2. O nome digitado deve bater exatamente com o nome da conta
     * <br>
     * A exclusão em cascata (membros, lavouras, despesas, convites)
     * é garantida pelos ON DELETE CASCADE definidos nas migrations.
     */
    @Transactional
    public void deleteAccount(UUID accountId, UUID userId, DeleteAccountRequest request) {
        // Verifica que o usuário é OWNER da conta
        AccountMember caller = accountMemberRepository
                .findByAccountIdAndUserId(accountId, userId)
                .orElseThrow(() -> new BusinessException("Acesso negado", HttpStatus.FORBIDDEN));

        if (caller.getRole() != AccountRole.OWNER) {
            throw new BusinessException(
                    "Apenas o OWNER pode excluir a conta", HttpStatus.FORBIDDEN);
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));

        // Valida que o nome digitado é idêntico ao nome da conta — case-sensitive
        // Mesma validação feita no frontend, repetida aqui como camada extra
        if (!account.getName().equals(request.confirmationName())) {
            throw new BusinessException(
                    "O nome digitado não confere com o nome da conta. Operação cancelada.",
                    HttpStatus.UNPROCESSABLE_CONTENT);
        }

        // A exclusão em cascata cuida de: account_members, account_invites,
        // farms → farm_activities, expenses, quotations, etc.
        accountRepository.delete(account);
    }
}