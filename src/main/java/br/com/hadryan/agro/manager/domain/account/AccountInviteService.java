package br.com.hadryan.agro.manager.domain.account;

import br.com.hadryan.agro.manager.domain.user.User;
import br.com.hadryan.agro.manager.domain.user.UserRepository;
import br.com.hadryan.agro.manager.infra.mail.EmailService;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de convites nominais por e-mail.
 * Apenas OWNER e ADMIN podem gerar e revogar convites.
 * O aceite exige que o e-mail do usuário autenticado coincida com o do convite.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountInviteService {

    private static final int INVITE_EXPIRY_DAYS = 7;

    private final AccountInviteRepository inviteRepository;
    private final AccountMemberRepository memberRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public AccountInviteResponse createInvite(UUID accountId, UUID userId, AccountInviteRequest request) {
        requireAdminOrOwner(accountId, userId);

        AccountRole role = request.roleOrDefault();
        if (role == AccountRole.OWNER) {
            throw new BusinessException("Não é permitido convidar diretamente como OWNER");
        }

        String invitedEmail = request.email().trim().toLowerCase();

        // Verifica se o e-mail já é membro da conta
        userRepository.findByEmailIgnoreCase(invitedEmail).ifPresent(existingUser -> {
            if (memberRepository.existsByAccountIdAndUserId(accountId, existingUser.getId())) {
                throw new BusinessException(
                        "O e-mail " + invitedEmail + " já é membro desta conta");
            }
        });

        // Verifica se já existe convite ativo para este e-mail nesta conta
        if (inviteRepository.existsActiveInviteByEmail(accountId, invitedEmail, LocalDateTime.now())) {
            throw new BusinessException(
                    "Já existe um convite ativo para " + invitedEmail + ". Revogue-o antes de enviar um novo.");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", userId));

        AccountInvite invite = AccountInvite.builder()
                .account(account)
                .token(UUID.randomUUID())
                .invitedEmail(invitedEmail)
                .role(role)
                .createdBy(creator)
                .expiresAt(LocalDateTime.now().plusDays(INVITE_EXPIRY_DAYS))
                .build();

        AccountInvite saved = inviteRepository.save(invite);
        AccountInviteResponse response = AccountInviteResponse.from(saved, frontendUrl);

        // Envia o e-mail — falhas são logadas mas não bloqueiam a criação do convite
        emailService.sendInviteEmail(
                invitedEmail,
                account.getName(),
                creator.getName(),
                role.name(),
                response.inviteUrl(),
                saved.getExpiresAt()
        );

        return response;
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

        if (!invite.getAccount().getId().equals(accountId)) {
            throw new ResourceNotFoundException("Convite", "id", inviteId);
        }

        inviteRepository.delete(invite);
    }

    @Transactional(readOnly = true)
    public AccountInviteResponse getInviteDetails(UUID token) {
        AccountInvite invite = findActiveInvite(token);
        return AccountInviteResponse.from(invite, frontendUrl);
    }

    /**
     * Aceita um convite e adiciona o usuário autenticado como membro da conta.
     * Valida que o e-mail do usuário autenticado coincide com o e-mail do convite.
     */
    @Transactional
    public AccountMemberResponse acceptInvite(UUID token, UUID userId) {
        AccountInvite invite = findActiveInvite(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", userId));

        // Valida que o usuário autenticado é o destinatário do convite
        if (invite.getInvitedEmail() != null &&
                !invite.getInvitedEmail().equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException(
                    "Este convite foi enviado para " + invite.getInvitedEmail() +
                            ". Faça login com o e-mail correto para aceitá-lo.",
                    HttpStatus.FORBIDDEN);
        }

        Account account = invite.getAccount();

        if (memberRepository.existsByAccountIdAndUserId(account.getId(), userId)) {
            throw new BusinessException("Você já é membro desta conta");
        }

        AccountMember member = AccountMember.builder()
                .account(account)
                .user(user)
                .role(invite.getRole())
                .build();

        memberRepository.save(member);

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