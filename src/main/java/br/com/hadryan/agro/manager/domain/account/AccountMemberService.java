package br.com.hadryan.agro.manager.domain.account;

import java.util.List;
import java.util.UUID;

/** Contrato do serviço de gerenciamento de membros de uma conta. */
public interface AccountMemberService {

    List<AccountMemberResponse> listMembers(UUID accountId, UUID userId);

    AccountMemberResponse updateRole(UUID accountId, UUID callerId, UUID memberId, AccountMemberRoleRequest request);

    void removeMember(UUID accountId, UUID callerId, UUID memberId);
}