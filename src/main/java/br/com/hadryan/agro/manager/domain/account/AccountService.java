package br.com.hadryan.agro.manager.domain.account;

import br.com.hadryan.agro.manager.domain.user.User;
import br.com.hadryan.agro.manager.domain.user.UserRepository;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Serviço responsável pelo gerenciamento de contas.
 * Criação da conta e consulta das contas do usuário autenticado.
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

        // Cria a conta com o usuário autenticado como dono
        Account account = Account.builder()
                .name(request.name())
                .owner(owner)
                .build();

        accountRepository.save(account);

        // Registra automaticamente o criador como membro com papel OWNER
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
}