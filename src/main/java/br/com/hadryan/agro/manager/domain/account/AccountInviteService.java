package br.com.hadryan.agro.manager.domain.account;

import java.util.List;
import java.util.UUID;

/** Contrato do serviço de convites nominais por e-mail. */
public interface AccountInviteService {

    AccountInviteResponse createInvite(UUID accountId, UUID userId, AccountInviteRequest request);

    List<AccountInviteResponse> listActiveInvites(UUID accountId, UUID userId);

    void revokeInvite(UUID accountId, UUID userId, UUID inviteId);

    AccountInviteResponse getInviteDetails(UUID token);

    AccountMemberResponse acceptInvite(UUID token, UUID userId);
}