package br.com.hadryan.agro.manager.domain.account;

import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Serviço de gerenciamento de membros de uma conta.
 * Regras de autorização:
 *  - Listar: qualquer membro pode ver a lista
 *  - Alterar papel / remover: apenas OWNER e ADMIN
 *  - Não é permitido alterar ou remover o OWNER
 *  - ADMIN não pode promover outro usuário a OWNER
 */
@Service
@RequiredArgsConstructor
public class AccountMemberService {

    private final AccountMemberRepository memberRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<AccountMemberResponse> listMembers(UUID accountId, UUID userId) {
        validateMembership(accountId, userId);
        return memberRepository.findByAccountIdWithUser(accountId)
                .stream()
                .map(AccountMemberResponse::from)
                .toList();
    }

    @Transactional
    public AccountMemberResponse updateRole(UUID accountId, UUID callerId, UUID memberId, AccountMemberRoleRequest request) {
        AccountMember caller = requireAdminOrOwner(accountId, callerId);
        AccountMember target = findMember(memberId, accountId);

        // Não é permitido alterar o papel do OWNER
        if (target.getRole() == AccountRole.OWNER) {
            throw new BusinessException("O papel do OWNER não pode ser alterado");
        }

        // ADMIN não pode promover para OWNER
        if (caller.getRole() == AccountRole.ADMIN && request.role() == AccountRole.OWNER) {
            throw new BusinessException("ADMIN não pode promover membros a OWNER", HttpStatus.FORBIDDEN);
        }

        target.setRole(request.role());
        return AccountMemberResponse.from(memberRepository.save(target));
    }

    @Transactional
    public void removeMember(UUID accountId, UUID callerId, UUID memberId) {
        requireAdminOrOwner(accountId, callerId);
        AccountMember target = findMember(memberId, accountId);

        // Não é permitido remover o OWNER da conta
        if (target.getRole() == AccountRole.OWNER) {
            throw new BusinessException("O OWNER não pode ser removido da conta");
        }

        memberRepository.delete(target);
    }

    // ── Utilitários privados ──────────────────────────────────────────────────

    private void validateMembership(UUID accountId, UUID userId) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));

        if (!memberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }
    }

    private AccountMember requireAdminOrOwner(UUID accountId, UUID userId) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));

        AccountMember caller = memberRepository.findByAccountIdAndUserId(accountId, userId)
                .orElseThrow(() -> new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN));

        if (caller.getRole() == AccountRole.MEMBER) {
            throw new BusinessException("Apenas OWNER e ADMIN podem gerenciar membros", HttpStatus.FORBIDDEN);
        }

        return caller;
    }

    private AccountMember findMember(UUID memberId, UUID accountId) {
        AccountMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro", "id", memberId));

        // Garante que o membro pertence à conta informada
        if (!member.getAccount().getId().equals(accountId)) {
            throw new ResourceNotFoundException("Membro", "id", memberId);
        }

        return member;
    }
}