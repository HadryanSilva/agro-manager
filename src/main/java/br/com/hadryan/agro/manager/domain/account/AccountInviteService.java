package br.com.hadryan.agro.manager.domain.account;

import br.com.hadryan.agro.manager.domain.user.User;
import br.com.hadryan.agro.manager.domain.user.UserRepository;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Serviço responsável pelo ciclo de vida dos convites de conta.
 * Apenas OWNER e ADMIN podem gerar e revogar convites.
 * O aceite pode ser feito por qualquer usuário autenticado que possua o token.
 */
@Service
@RequiredArgsConstructor
public class AccountInviteService {

    // Validade padrão de um convite em dias
    private static final int INVITE_EXPIRY_DAYS = 7;

    private final AccountInviteRepository inviteRepository;
    private final AccountMemberRepository memberRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public AccountInviteResponse createInvite(UUID accountId, UUID userId, AccountInviteRequest request) {
        requireAdminOrOwner(accountId, userId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", userId));

        // Não permite gerar convite para papel OWNER via API
        AccountRole role = request.roleOrDefault();
        if (role == AccountRole.OWNER) {
            throw new BusinessException("Não é permitido convidar diretamente como OWNER");
        }

        AccountInvite invite = AccountInvite.builder()
                .account(account)
                .token(UUID.randomUUID())
                .role(role)
                .createdBy(creator)
                .expiresAt(LocalDateTime.now().plusDays(INVITE_EXPIRY_DAYS))
                .build();

        return AccountInviteResponse.from(inviteRepository.save(invite), frontendUrl);
    }

    @Transactional(readOnly = true)
    public List<AccountInviteResponse> listActiveInvites(UUID accountId, UUID userId) {
        requireAdminOrOwner(accountId, userId);
        return inviteRepository.findActiveByAccountId(accountId, LocalDateTime.now())
                .stream()
                .map(i -> AccountInviteResponse.from(i, frontendUrl))
                .toList();
    }

    @Transactional
    public void revokeInvite(UUID accountId, UUID userId, UUID inviteId) {
        requireAdminOrOwner(accountId, userId);

        AccountInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Convite", "id", inviteId));

        // Garante que o convite pertence à conta informada
        if (!invite.getAccount().getId().equals(accountId)) {
            throw new ResourceNotFoundException("Convite", "id", inviteId);
        }

        inviteRepository.delete(invite);
    }

    /**
     * Retorna detalhes de um convite pelo token — endpoint público para o frontend
     * exibir informações antes do usuário fazer login.
     */
    @Transactional(readOnly = true)
    public AccountInviteResponse getInviteDetails(UUID token) {
        AccountInvite invite = findActiveInvite(token);
        return AccountInviteResponse.from(invite, frontendUrl);
    }

    /**
     * Aceita um convite e adiciona o usuário autenticado como membro da conta.
     * Idempotente — se o usuário já for membro, apenas marca o convite como utilizado.
     */
    @Transactional
    public AccountMemberResponse acceptInvite(UUID token, UUID userId) {
        AccountInvite invite = findActiveInvite(token);
        Account account = invite.getAccount();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", userId));

        // Verifica se o usuário já é membro da conta
        if (memberRepository.existsByAccountIdAndUserId(account.getId(), userId)) {
            throw new BusinessException("Você já é membro desta conta");
        }

        // Adiciona o usuário como membro com o papel definido no convite
        AccountMember member = AccountMember.builder()
                .account(account)
                .user(user)
                .role(invite.getRole())
                .build();

        memberRepository.save(member);

        // Marca o convite como utilizado
        invite.setUsedAt(LocalDateTime.now());
        inviteRepository.save(invite);

        return AccountMemberResponse.from(member);
    }

    // ── Utilitários privados ──────────────────────────────────────────────────

    private AccountInvite findActiveInvite(UUID token) {
        AccountInvite invite = inviteRepository.findByTokenWithDetails(token)
                .orElseThrow(() -> new ResourceNotFoundException("Convite", "token", token));

        if (invite.isUsed()) {
            throw new BusinessException("Este convite já foi utilizado", HttpStatus.GONE);
        }

        if (invite.isExpired()) {
            throw new BusinessException("Este convite expirou", HttpStatus.GONE);
        }

        return invite;
    }

    private void requireAdminOrOwner(UUID accountId, UUID userId) {
        AccountMember caller = memberRepository.findByAccountIdAndUserId(accountId, userId)
                .orElseThrow(() -> new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN));

        if (caller.getRole() == AccountRole.MEMBER) {
            throw new BusinessException("Apenas OWNER e ADMIN podem gerenciar convites", HttpStatus.FORBIDDEN);
        }
    }
}