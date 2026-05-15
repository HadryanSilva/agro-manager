package br.com.hadryan.agro.manager.domain.trading;

import br.com.hadryan.agro.manager.domain.account.Account;
import br.com.hadryan.agro.manager.domain.account.AccountMemberRepository;
import br.com.hadryan.agro.manager.domain.account.AccountRepository;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradingClientService {

    private final TradingClientRepository clientRepository;
    private final ClientOrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;

    @Transactional
    public TradingClientResponse create(UUID accountId, UUID userId, TradingClientRequest request) {
        Account account = validateAndGetAccount(accountId, userId);
        TradingClient client = TradingClient.builder()
                .account(account)
                .name(request.name().trim())
                .phone(request.phone())
                .city(request.city())
                .notes(request.notes())
                .build();
        return TradingClientResponse.from(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public List<TradingClientResponse> listAll(UUID accountId, UUID userId) {
        validateMembership(accountId, userId);
        return clientRepository.findByAccountIdOrderByNameAsc(accountId).stream()
                .map(TradingClientResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TradingClientResponse> search(UUID accountId, UUID userId, String name) {
        validateMembership(accountId, userId);
        return clientRepository.searchByName(accountId, name).stream()
                .map(TradingClientResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TradingClientResponse findById(UUID accountId, UUID userId, UUID clientId) {
        validateMembership(accountId, userId);
        return TradingClientResponse.from(findClient(clientId, accountId));
    }

    @Transactional
    public TradingClientResponse update(UUID accountId, UUID userId, UUID clientId, TradingClientRequest request) {
        validateMembership(accountId, userId);
        TradingClient client = findClient(clientId, accountId);
        client.setName(request.name().trim());
        client.setPhone(request.phone());
        client.setCity(request.city());
        client.setNotes(request.notes());
        return TradingClientResponse.from(clientRepository.save(client));
    }

    @Transactional
    public void delete(UUID accountId, UUID userId, UUID clientId) {
        validateMembership(accountId, userId);
        TradingClient client = findClient(clientId, accountId);
        if (orderRepository.existsByClientId(clientId)) {
            throw new BusinessException(
                    "Não é possível excluir um cliente com pedidos registrados.",
                    HttpStatus.CONFLICT
            );
        }
        clientRepository.delete(client);
    }

    private Account validateAndGetAccount(UUID accountId, UUID userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));
        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }
        return account;
    }

    private void validateMembership(UUID accountId, UUID userId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Conta", "id", accountId);
        }
        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }
    }

    private TradingClient findClient(UUID clientId, UUID accountId) {
        return clientRepository.findByIdAndAccountId(clientId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", clientId));
    }
}
