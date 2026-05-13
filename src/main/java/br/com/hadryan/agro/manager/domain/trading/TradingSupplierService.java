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

/**
 * Serviço de gerenciamento de fornecedores do modo comprador.
 */
@Service
@RequiredArgsConstructor
public class TradingSupplierService {

    private final TradingSupplierRepository supplierRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;

    @Transactional
    public TradingSupplierResponse create(UUID accountId, UUID userId, TradingSupplierRequest request) {
        Account account = validateAndGetAccount(accountId, userId);

        TradingSupplier supplier = TradingSupplier.builder()
                .account(account)
                .name(request.name().trim())
                .phone(request.phone())
                .city(request.city())
                .notes(request.notes())
                .build();

        return TradingSupplierResponse.from(supplierRepository.save(supplier));
    }

    @Transactional(readOnly = true)
    public List<TradingSupplierResponse> listAll(UUID accountId, UUID userId) {
        validateMembership(accountId, userId);
        return supplierRepository.findByAccountIdOrderByNameAsc(accountId).stream()
                .map(TradingSupplierResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TradingSupplierResponse> search(UUID accountId, UUID userId, String name) {
        validateMembership(accountId, userId);
        return supplierRepository.searchByName(accountId, name).stream()
                .map(TradingSupplierResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TradingSupplierResponse findById(UUID accountId, UUID userId, UUID supplierId) {
        validateMembership(accountId, userId);
        TradingSupplier supplier = findSupplier(supplierId, accountId);
        return TradingSupplierResponse.from(supplier);
    }

    @Transactional
    public TradingSupplierResponse update(UUID accountId, UUID userId, UUID supplierId, TradingSupplierRequest request) {
        validateMembership(accountId, userId);
        TradingSupplier supplier = findSupplier(supplierId, accountId);

        supplier.setName(request.name().trim());
        supplier.setPhone(request.phone());
        supplier.setCity(request.city());
        supplier.setNotes(request.notes());

        return TradingSupplierResponse.from(supplierRepository.save(supplier));
    }

    @Transactional
    public void delete(UUID accountId, UUID userId, UUID supplierId) {
        validateMembership(accountId, userId);
        TradingSupplier supplier = findSupplier(supplierId, accountId);

        // Impede exclusão de fornecedor com lotes vinculados — dados históricos devem ser preservados
        if (supplierRepository.hasAssociatedLots(supplierId)) {
            throw new BusinessException(
                    "Não é possível excluir um fornecedor com lotes de compra registrados.",
                    HttpStatus.CONFLICT
            );
        }

        supplierRepository.delete(supplier);
    }

    // ── Utilitários privados ──────────────────────────────────────────────────

    /**
     * Valida acesso e retorna a conta. Uma única query para conta + uma para membership.
     * Evita a query redundante que existia ao chamar validateMembership() após findById().
     */
    private Account validateAndGetAccount(UUID accountId, UUID userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));
        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }
        return account;
    }

    /**
     * Valida acesso sem retornar a conta. Usa existsById para evitar carregar a entidade.
     */
    private void validateMembership(UUID accountId, UUID userId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Conta", "id", accountId);
        }
        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }
    }

    private TradingSupplier findSupplier(UUID supplierId, UUID accountId) {
        return supplierRepository.findByIdAndAccountId(supplierId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", "id", supplierId));
    }
}
