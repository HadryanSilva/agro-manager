package br.com.hadryan.agro.manager.domain.quotation;

import br.com.hadryan.agro.manager.domain.account.Account;
import br.com.hadryan.agro.manager.domain.account.AccountMemberRepository;
import br.com.hadryan.agro.manager.domain.account.AccountRepository;
import br.com.hadryan.agro.manager.domain.farm.Farm;
import br.com.hadryan.agro.manager.domain.farm.FarmRepository;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de gerenciamento de cotações de insumos.
 * Agrupa cotações por nome de produto e calcula métricas de economia.
 */
@Service
@RequiredArgsConstructor
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;
    private final FarmRepository farmRepository;

    @Transactional
    public QuotationResponse create(UUID accountId, UUID userId, QuotationRequest request) {
        Account account = validateAndGetAccount(accountId, userId);

        Farm farm = null;
        if (request.farmId() != null) {
            farm = farmRepository.findByIdAndAccountId(request.farmId(), accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lavoura", "id", request.farmId()));
        }

        Quotation quotation = Quotation.builder()
                .account(account)
                .farm(farm)
                .productName(request.productName().trim())
                .supplier(request.supplier().trim())
                .unitPrice(request.unitPrice())
                .quantity(request.quantity())
                .unit(request.unit())
                .quotationDate(request.quotationDate())
                .notes(request.notes())
                .build();

        return QuotationResponse.from(quotationRepository.save(quotation));
    }

    @Transactional(readOnly = true)
    public List<QuotationGroupResponse> listGrouped(UUID accountId, UUID userId) {
        validateAndGetAccount(accountId, userId);

        List<Quotation> all = quotationRepository.findByAccountIdOrderByProductAndPrice(accountId);

        // Agrupa por nome do produto (case-insensitive)
        Map<String, List<Quotation>> grouped = all.stream()
                .collect(Collectors.groupingBy(q -> q.getProductName().toLowerCase()));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> buildGroup(entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getProductSuggestions(UUID accountId, UUID userId) {
        validateAndGetAccount(accountId, userId);
        return quotationRepository.findDistinctProductNamesByAccountId(accountId);
    }

    @Transactional
    public QuotationResponse update(UUID accountId, UUID userId, UUID quotationId, QuotationRequest request) {
        validateAndGetAccount(accountId, userId);

        Quotation quotation = quotationRepository.findByIdAndAccountId(quotationId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotação", "id", quotationId));

        Farm farm = null;
        if (request.farmId() != null) {
            farm = farmRepository.findByIdAndAccountId(request.farmId(), accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lavoura", "id", request.farmId()));
        }

        quotation.setProductName(request.productName().trim());
        quotation.setSupplier(request.supplier().trim());
        quotation.setUnitPrice(request.unitPrice());
        quotation.setQuantity(request.quantity());
        quotation.setUnit(request.unit());
        quotation.setQuotationDate(request.quotationDate());
        quotation.setNotes(request.notes());
        quotation.setFarm(farm);

        return QuotationResponse.from(quotationRepository.save(quotation));
    }

    @Transactional
    public void delete(UUID accountId, UUID userId, UUID quotationId) {
        validateAndGetAccount(accountId, userId);

        Quotation quotation = quotationRepository.findByIdAndAccountId(quotationId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotação", "id", quotationId));

        quotationRepository.delete(quotation);
    }

    // ── Utilitários privados ──────────────────────────────────────────────────

    private Account validateAndGetAccount(UUID accountId, UUID userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));

        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }

        return account;
    }

    private QuotationGroupResponse buildGroup(List<Quotation> quotations) {
        // Ordena por preço unitário crescente
        List<Quotation> sorted = quotations.stream()
                .sorted(Comparator.comparing(Quotation::getUnitPrice))
                .toList();

        Quotation cheapest = sorted.get(0);
        Quotation mostExpensive = sorted.get(sorted.size() - 1);

        BigDecimal minPrice = cheapest.getUnitPrice();
        BigDecimal maxPrice = mostExpensive.getUnitPrice();

        BigDecimal avgPrice = quotations.stream()
                .map(Quotation::getUnitPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(quotations.size()), 2, RoundingMode.HALF_UP);

        // Economia = (preço mais caro - preço mais barato) × quantidade do mais barato
        BigDecimal savings = maxPrice.subtract(minPrice).multiply(cheapest.getQuantity());

        return new QuotationGroupResponse(
                cheapest.getProductName(),
                quotations.size(),
                minPrice,
                maxPrice,
                avgPrice,
                savings.compareTo(BigDecimal.ZERO) > 0 ? savings : BigDecimal.ZERO,
                cheapest.getSupplier(),
                sorted.stream().map(QuotationResponse::from).toList()
        );
    }
}