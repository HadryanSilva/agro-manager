package br.com.hadryan.agro.manager.domain.account;

import java.util.List;
import java.util.UUID;

/**
 * Contrato do serviço de gerenciamento de contas.
 * A separação interface/implementação permite substituição de implementações
 * e isolamento em testes unitários via mock.
 */
public interface AccountService {

    AccountResponse createAccount(UUID userId, CreateAccountRequest request);

    List<AccountResponse> getUserAccounts(UUID userId);

    void deleteAccount(UUID accountId, UUID userId, DeleteAccountRequest request);
}