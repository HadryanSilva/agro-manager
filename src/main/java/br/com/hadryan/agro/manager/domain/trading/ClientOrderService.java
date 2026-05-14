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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientOrderService {

    private final ClientOrderRepository orderRepository;
    private final OrderSupplierLegRepository legRepository;
    private final OrderTruckRepository truckRepository;
    private final TradingClientRepository clientRepository;
    private final TradingSupplierRepository supplierRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;

    @Transactional
    public ClientOrderDetailResponse createOrder(UUID accountId, UUID userId, ClientOrderRequest request) {
        Account account = validateAndGetAccount(accountId, userId);

        TradingClient client = clientRepository.findByIdAndAccountId(request.clientId(), accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", request.clientId()));

        ClientOrder order = ClientOrder.builder()
                .account(account)
                .client(client)
                .orderDate(request.orderDate())
                .clientPricePerKg(request.clientPricePerKg())
                .notes(request.notes())
                .build();

        addLegsToOrder(order, request.legs(), accountId);

        ClientOrder saved = orderRepository.save(order);
        return loadDetail(saved.getId(), accountId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientOrderSummaryResponse> listOrders(
            UUID accountId, UUID userId,
            ClientOrderStatus status, UUID clientId,
            int page, int size) {

        validateMembership(accountId, userId);
        Pageable pageable = PageRequest.of(page, size);

        Page<ClientOrder> orders;
        if (status != null && clientId != null) {
            orders = orderRepository.findByAccountIdAndStatusAndClientIdWithClient(accountId, status, clientId, pageable);
        } else if (status != null) {
            orders = orderRepository.findByAccountIdAndStatusWithClient(accountId, status, pageable);
        } else if (clientId != null) {
            orders = orderRepository.findByAccountIdAndClientIdWithClient(accountId, clientId, pageable);
        } else {
            orders = orderRepository.findByAccountIdWithClient(accountId, pageable);
        }

        List<UUID> orderIds = orders.getContent().stream().map(ClientOrder::getId).toList();

        Map<UUID, BigDecimal> kgMap = orderIds.isEmpty() ? Map.of()
                : orderRepository.sumTotalKgByOrderIds(orderIds).stream()
                        .collect(Collectors.toMap(r -> (UUID) r[0], r -> (BigDecimal) r[1]));

        Map<UUID, BigDecimal> costMap = orderIds.isEmpty() ? Map.of()
                : orderRepository.sumTotalCostByOrderIds(orderIds).stream()
                        .collect(Collectors.toMap(r -> (UUID) r[0], r -> (BigDecimal) r[1]));

        List<ClientOrderSummaryResponse> content = orders.getContent().stream()
                .map(o -> ClientOrderSummaryResponse.from(
                        o,
                        kgMap.getOrDefault(o.getId(), BigDecimal.ZERO),
                        costMap.getOrDefault(o.getId(), BigDecimal.ZERO)))
                .toList();

        return new PageResponse<>(content, orders.getNumber(), orders.getSize(),
                orders.getTotalElements(), orders.getTotalPages(), orders.isLast(), null);
    }

    @Transactional(readOnly = true)
    public ClientOrderDetailResponse getOrderDetail(UUID accountId, UUID userId, UUID orderId) {
        validateMembership(accountId, userId);
        return loadDetail(orderId, accountId);
    }

    @Transactional
    public ClientOrderDetailResponse updateOrder(UUID accountId, UUID userId, UUID orderId, ClientOrderRequest request) {
        validateMembership(accountId, userId);

        ClientOrder order = orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));

        if (order.getStatus() == ClientOrderStatus.CLOSED) {
            throw new BusinessException("Pedido encerrado não pode ser editado.", HttpStatus.CONFLICT);
        }

        TradingClient client = clientRepository.findByIdAndAccountId(request.clientId(), accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", request.clientId()));

        order.setClient(client);
        order.setOrderDate(request.orderDate());
        order.setClientPricePerKg(request.clientPricePerKg());
        order.setNotes(request.notes());

        order.getLegs().clear();
        addLegsToOrder(order, request.legs(), accountId);

        orderRepository.save(order);
        return loadDetail(orderId, accountId);
    }

    @Transactional
    public void closeOrder(UUID accountId, UUID userId, UUID orderId) {
        validateMembership(accountId, userId);
        ClientOrder order = orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));
        if (order.getStatus() == ClientOrderStatus.CLOSED) return;
        order.setStatus(ClientOrderStatus.CLOSED);
        orderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(UUID accountId, UUID userId, UUID orderId) {
        validateMembership(accountId, userId);
        ClientOrder order = orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));
        if (order.getStatus() == ClientOrderStatus.CLOSED) {
            throw new BusinessException("Pedido encerrado não pode ser excluído.", HttpStatus.CONFLICT);
        }
        orderRepository.delete(order);
    }

    @Transactional
    public ClientOrderDetailResponse addLeg(UUID accountId, UUID userId, UUID orderId, OrderSupplierLegRequest request) {
        validateMembership(accountId, userId);

        ClientOrder order = orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));

        if (order.getStatus() == ClientOrderStatus.CLOSED) {
            throw new BusinessException("Não é possível adicionar pernas a um pedido encerrado.", HttpStatus.CONFLICT);
        }

        TradingSupplier supplier = supplierRepository.findByIdAndAccountId(request.supplierId(), accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", "id", request.supplierId()));

        OrderSupplierLeg leg = OrderSupplierLeg.builder()
                .order(order)
                .supplier(supplier)
                .supplierPricePerKg(request.supplierPricePerKg())
                .notes(request.notes())
                .build();

        request.trucks().forEach(t -> leg.getTrucks().add(OrderTruck.builder()
                .leg(leg)
                .truckPlate(t.truckPlate().toUpperCase().trim())
                .quantityKg(t.quantityKg())
                .freightValue(t.freightValue())
                .notes(t.notes())
                .build()));

        order.getLegs().add(leg);
        orderRepository.save(order);
        return loadDetail(orderId, accountId);
    }

    @Transactional
    public void removeLeg(UUID accountId, UUID userId, UUID orderId, UUID legId) {
        validateMembership(accountId, userId);

        ClientOrder order = orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));

        if (order.getStatus() == ClientOrderStatus.CLOSED) {
            throw new BusinessException("Não é possível remover pernas de um pedido encerrado.", HttpStatus.CONFLICT);
        }

        OrderSupplierLeg leg = legRepository.findByIdAndOrderId(legId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Perna de fornecedor", "id", legId));

        order.getLegs().remove(leg);
        orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public TradingDashboardResponse getDashboard(UUID accountId, UUID userId) {
        validateMembership(accountId, userId);

        long totalOrders  = orderRepository.countByAccountId(accountId);
        long openOrders   = orderRepository.countByAccountIdAndStatus(accountId, ClientOrderStatus.OPEN);
        long closedOrders = orderRepository.countByAccountIdAndStatus(accountId, ClientOrderStatus.CLOSED);
        long totalClients    = clientRepository.countByAccountId(accountId);
        long totalSuppliers  = supplierRepository.countByAccountId(accountId);

        BigDecimal totalKg      = orderRepository.sumTotalKgByAccountId(accountId);
        BigDecimal totalRevenue = orderRepository.sumRevenueByAccountId(accountId);
        BigDecimal productCost  = orderRepository.sumProductCostByAccountId(accountId);
        BigDecimal freightCost  = orderRepository.sumFreightCostByAccountId(accountId);
        BigDecimal totalCost    = productCost.add(freightCost);

        return new TradingDashboardResponse(
                totalOrders, openOrders, closedOrders,
                totalKg, totalRevenue, totalCost,
                totalRevenue.subtract(totalCost),
                totalClients, totalSuppliers
        );
    }

    private void addLegsToOrder(ClientOrder order, List<OrderSupplierLegRequest> legRequests, UUID accountId) {
        legRequests.forEach(lr -> {
            TradingSupplier supplier = supplierRepository.findByIdAndAccountId(lr.supplierId(), accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", "id", lr.supplierId()));

            OrderSupplierLeg leg = OrderSupplierLeg.builder()
                    .order(order)
                    .supplier(supplier)
                    .supplierPricePerKg(lr.supplierPricePerKg())
                    .notes(lr.notes())
                    .build();

            lr.trucks().forEach(t -> leg.getTrucks().add(OrderTruck.builder()
                    .leg(leg)
                    .truckPlate(t.truckPlate().toUpperCase().trim())
                    .quantityKg(t.quantityKg())
                    .freightValue(t.freightValue())
                    .notes(t.notes())
                    .build()));

            order.getLegs().add(leg);
        });
    }

    private ClientOrderDetailResponse loadDetail(UUID orderId, UUID accountId) {
        ClientOrder order = orderRepository.findWithLegsByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));
        List<OrderTruck> trucks = truckRepository.findByLegOrderId(orderId);
        trucks.forEach(t -> order.getLegs().stream()
                .filter(l -> l.getId().equals(t.getLeg().getId()))
                .findFirst()
                .ifPresent(l -> {
                    if (!l.getTrucks().contains(t)) l.getTrucks().add(t);
                }));
        return ClientOrderDetailResponse.from(order);
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
}
