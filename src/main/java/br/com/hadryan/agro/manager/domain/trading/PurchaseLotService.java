package br.com.hadryan.agro.manager.domain.trading;

import br.com.hadryan.agro.manager.domain.account.Account;
import br.com.hadryan.agro.manager.domain.account.AccountMemberRepository;
import br.com.hadryan.agro.manager.domain.account.AccountRepository;
import br.com.hadryan.agro.manager.shared.dto.PageResponse;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço de lotes de compra e suas vendas.
 * Responsável por criar lotes, registrar caminhões de compra,
 * registrar vendas e atualizar o status do lote automaticamente.
 */
@Service
@RequiredArgsConstructor
public class PurchaseLotService {

    private final PurchaseLotRepository lotRepository;
    private final LotSaleRepository saleRepository;
    private final TradingSupplierRepository supplierRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;

    // ── Lotes de compra ───────────────────────────────────────────────────────

    @Transactional
    public PurchaseLotDetailResponse createLot(UUID accountId, UUID userId, PurchaseLotRequest request) {
        Account account = validateAndGetAccount(accountId, userId);

        TradingSupplier supplier = supplierRepository.findByIdAndAccountId(request.supplierId(), accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", "id", request.supplierId()));

        PurchaseLot lot = PurchaseLot.builder()
                .account(account)
                .supplier(supplier)
                .purchaseDate(request.purchaseDate())
                .pricePerKg(request.pricePerKg())
                .notes(request.notes())
                .build();

        // Adiciona os caminhões ao lote
        request.trucks().forEach(t -> {
            PurchaseTruck truck = PurchaseTruck.builder()
                    .lot(lot)
                    .truckPlate(t.truckPlate().toUpperCase().trim())
                    .quantityKg(t.quantityKg())
                    .notes(t.notes())
                    .build();
            lot.getTrucks().add(truck);
        });

        PurchaseLot saved = lotRepository.save(lot);
        // Lote recém-criado não tem vendas ainda — lista vazia segura
        return PurchaseLotDetailResponse.from(saved, List.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseLotSummaryResponse> listLots(UUID accountId, UUID userId,
                                                             PurchaseLotStatus status,
                                                             UUID supplierId,
                                                             int page, int size) {
        validateMembership(accountId, userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("purchaseDate").descending());
        Page<PurchaseLot> lots;

        if (supplierId != null) {
            lots = lotRepository.findByAccountIdAndSupplierIdWithSupplier(accountId, supplierId, pageable);
        } else if (status != null) {
            lots = lotRepository.findByAccountIdAndStatusWithSupplier(accountId, status, pageable);
        } else {
            lots = lotRepository.findByAccountIdWithSupplier(accountId, pageable);
        }

        // Uma query por agregação para todos os lotes da página — evita N+1
        List<UUID> lotIds = lots.getContent().stream().map(PurchaseLot::getId).toList();
        Map<UUID, BigDecimal> purchasedKgMap = lotIds.isEmpty() ? Map.of()
                : lotRepository.sumPurchasedKgByLotIds(lotIds).stream()
                        .collect(Collectors.toMap(r -> (UUID) r[0], r -> (BigDecimal) r[1]));
        Map<UUID, BigDecimal> soldKgMap = lotIds.isEmpty() ? Map.of()
                : saleRepository.sumSoldKgByLotIds(lotIds).stream()
                        .collect(Collectors.toMap(r -> (UUID) r[0], r -> (BigDecimal) r[1]));

        List<PurchaseLotSummaryResponse> content = lots.getContent().stream()
                .map(lot -> PurchaseLotSummaryResponse.from(lot,
                        purchasedKgMap.getOrDefault(lot.getId(), BigDecimal.ZERO),
                        soldKgMap.getOrDefault(lot.getId(), BigDecimal.ZERO)))
                .toList();

        // totalValue não se aplica a lotes — null mantém o contrato do record
        return new PageResponse<>(content, lots.getNumber(), lots.getSize(),
                lots.getTotalElements(), lots.getTotalPages(), lots.isLast(), null);
    }

    @Transactional(readOnly = true)
    public PurchaseLotDetailResponse getLotDetail(UUID accountId, UUID userId, UUID lotId) {
        validateMembership(accountId, userId);

        // Carrega lote com supplier + trucks de compra (uma query)
        PurchaseLot lot = lotRepository.findWithTrucksByIdAndAccountId(lotId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote", "id", lotId));

        // Carrega vendas com trucks de entrega em query separada — evita MultipleBagFetchException
        List<LotSale> sales = saleRepository.findByLotIdWithTrucks(lotId);

        return PurchaseLotDetailResponse.from(lot, sales);
    }

    @Transactional
    public PurchaseLotDetailResponse updateLot(UUID accountId, UUID userId, UUID lotId, PurchaseLotRequest request) {
        validateMembership(accountId, userId);

        PurchaseLot lot = lotRepository.findByIdAndAccountId(lotId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote", "id", lotId));

        if (lot.getStatus() == PurchaseLotStatus.CLOSED) {
            throw new BusinessException("Lote encerrado não pode ser editado.", HttpStatus.CONFLICT);
        }

        TradingSupplier supplier = supplierRepository.findByIdAndAccountId(request.supplierId(), accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", "id", request.supplierId()));

        lot.setSupplier(supplier);
        lot.setPurchaseDate(request.purchaseDate());
        lot.setPricePerKg(request.pricePerKg());
        lot.setNotes(request.notes());

        // Substitui caminhões de compra (orphanRemoval cuida da exclusão dos antigos)
        lot.getTrucks().clear();
        request.trucks().forEach(t -> {
            PurchaseTruck truck = PurchaseTruck.builder()
                    .lot(lot)
                    .truckPlate(t.truckPlate().toUpperCase().trim())
                    .quantityKg(t.quantityKg())
                    .notes(t.notes())
                    .build();
            lot.getTrucks().add(truck);
        });

        lotRepository.save(lot);

        // Recarrega com JOIN FETCH diretamente — evita re-validar membership
        PurchaseLot updated = lotRepository.findWithTrucksByIdAndAccountId(lotId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote", "id", lotId));
        List<LotSale> sales = saleRepository.findByLotIdWithTrucks(lotId);
        return PurchaseLotDetailResponse.from(updated, sales);
    }

    @Transactional
    public void closeLot(UUID accountId, UUID userId, UUID lotId) {
        validateMembership(accountId, userId);
        PurchaseLot lot = lotRepository.findByIdAndAccountId(lotId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote", "id", lotId));
        if (lot.getStatus() == PurchaseLotStatus.CLOSED) {
            return;
        }
        lot.setStatus(PurchaseLotStatus.CLOSED);
        lotRepository.save(lot);
    }

    @Transactional
    public void deleteLot(UUID accountId, UUID userId, UUID lotId) {
        validateMembership(accountId, userId);
        PurchaseLot lot = lotRepository.findByIdAndAccountId(lotId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote", "id", lotId));
        if (saleRepository.existsByLotId(lotId)) {
            throw new BusinessException("Lote com vendas registradas não pode ser excluído.", HttpStatus.CONFLICT);
        }
        lotRepository.delete(lot);
    }

    // ── Vendas de lotes ───────────────────────────────────────────────────────

    @Transactional
    public LotSaleResponse createSale(UUID accountId, UUID userId, UUID lotId, LotSaleRequest request) {
        Account account = validateAndGetAccount(accountId, userId);

        PurchaseLot lot = lotRepository.findByIdAndAccountId(lotId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote", "id", lotId));

        if (lot.getStatus() == PurchaseLotStatus.CLOSED) {
            throw new BusinessException("Este lote está encerrado e não aceita novas vendas.", HttpStatus.CONFLICT);
        }

        LotSale sale = LotSale.builder()
                .account(account)
                .lot(lot)
                .buyerName(request.buyerName().trim())
                .saleDate(request.saleDate())
                .pricePerKg(request.pricePerKg())
                .notes(request.notes())
                .build();

        request.trucks().forEach(t -> {
            LotSaleTruck truck = LotSaleTruck.builder()
                    .sale(sale)
                    .truckPlate(t.truckPlate().toUpperCase().trim())
                    .quantityKg(t.quantityKg())
                    .notes(t.notes())
                    .build();
            sale.getTrucks().add(truck);
        });

        LotSale saved = saleRepository.save(sale);

        // Verifica se o lote foi totalmente vendido e fecha automaticamente
        updateLotStatusAfterSale(lot);

        return LotSaleResponse.from(saved);
    }

    @Transactional
    public void deleteSale(UUID accountId, UUID userId, UUID lotId, UUID saleId) {
        validateMembership(accountId, userId);

        LotSale sale = saleRepository.findByIdAndAccountId(saleId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", "id", saleId));

        if (!sale.getLot().getId().equals(lotId)) {
            throw new BusinessException("Venda não pertence a este lote.", HttpStatus.BAD_REQUEST);
        }

        PurchaseLot lot = sale.getLot();
        saleRepository.delete(sale);

        // Reabre o lote caso estivesse fechado — há estoque novamente
        if (lot.getStatus() == PurchaseLotStatus.CLOSED) {
            lot.setStatus(PurchaseLotStatus.OPEN);
            lotRepository.save(lot);
        }
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TradingDashboardResponse getDashboard(UUID accountId, UUID userId) {
        validateMembership(accountId, userId);

        // Contagens via queries diretas — sem carregar entidades em memória
        long totalLots   = lotRepository.countByAccountId(accountId);
        long openLots    = lotRepository.countByAccountIdAndStatus(accountId, PurchaseLotStatus.OPEN);
        long closedLots  = lotRepository.countByAccountIdAndStatus(accountId, PurchaseLotStatus.CLOSED);
        long totalSuppliers = supplierRepository.countByAccountId(accountId);

        // Totais de compra — queries escalares independentes evitam ambiguidade do Object[]
        BigDecimal totalPurchasedKg = lotRepository.sumPurchasedKgByAccountId(accountId);
        BigDecimal totalCost        = lotRepository.sumPurchasedCostByAccountId(accountId);

        // Totais de venda — queries escalares independentes
        BigDecimal totalSoldKg  = saleRepository.sumSoldKgByAccountId(accountId);
        BigDecimal totalRevenue = saleRepository.sumRevenueByAccountId(accountId);

        return new TradingDashboardResponse(
                totalLots, openLots, closedLots,
                totalPurchasedKg, totalSoldKg,
                totalCost, totalRevenue,
                totalRevenue.subtract(totalCost),
                totalSuppliers
        );
    }

    // ── Utilitários privados ──────────────────────────────────────────────────

    /**
     * Fecha o lote automaticamente quando o total vendido atingir ou superar o total comprado.
     * Usa queries de agregação — não acessa coleções lazy do lote.
     */
    private void updateLotStatusAfterSale(PurchaseLot lot) {
        BigDecimal totalPurchasedKg = lotRepository.sumPurchasedKgByLotId(lot.getId());
        BigDecimal totalSoldKg      = saleRepository.sumSoldKgByLotId(lot.getId());

        if (totalSoldKg.compareTo(totalPurchasedKg) >= 0) {
            lot.setStatus(PurchaseLotStatus.CLOSED);
            lotRepository.save(lot);
        }
    }

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

}