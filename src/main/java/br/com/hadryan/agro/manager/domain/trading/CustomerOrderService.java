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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    private final CustomerOrderRepository orderRepository;
    private final PurchaseLotRepository lotRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;

    @Transactional
    public CustomerOrderResponse create(UUID accountId, UUID userId, CustomerOrderRequest request) {
        Account account = validateAndGetAccount(accountId, userId);

        CustomerOrder order = CustomerOrder.builder()
                .account(account)
                .customerName(request.customerName().trim())
                .customerPhone(request.customerPhone())
                .customerDocument(request.customerDocument())
                .quantityKg(request.quantityKg())
                .pricePerKg(request.pricePerKg())
                .product(request.product().trim())
                .orderDate(request.orderDate())
                .deliveryDeadline(request.deliveryDeadline())
                .notes(request.notes())
                .build();

        return CustomerOrderResponse.from(orderRepository.save(order), CustomerOrderStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderResponse> listAll(UUID accountId, UUID userId, CustomerOrderStatus statusFilter) {
        validateMembership(accountId, userId);

        List<CustomerOrder> orders = orderRepository.findByAccountIdOrderByOrderDateDesc(accountId);
        Set<UUID> fulfilledIds = new HashSet<>(lotRepository.findCustomerOrderIdsByAccountId(accountId));

        return orders.stream()
                .map(o -> {
                    CustomerOrderStatus status = fulfilledIds.contains(o.getId())
                            ? CustomerOrderStatus.FULFILLED
                            : CustomerOrderStatus.PENDING;
                    return CustomerOrderResponse.from(o, status);
                })
                .filter(r -> statusFilter == null || r.status() == statusFilter)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerOrderResponse findById(UUID accountId, UUID userId, UUID orderId) {
        validateMembership(accountId, userId);

        CustomerOrder order = orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));

        CustomerOrderStatus status = lotRepository.existsByCustomerOrderId(orderId)
                ? CustomerOrderStatus.FULFILLED
                : CustomerOrderStatus.PENDING;

        return CustomerOrderResponse.from(order, status);
    }

    @Transactional
    public CustomerOrderResponse update(UUID accountId, UUID userId, UUID orderId, CustomerOrderRequest request) {
        validateMembership(accountId, userId);

        CustomerOrder order = orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));

        if (lotRepository.existsByCustomerOrderId(orderId)) {
            throw new BusinessException(
                    "Pedido já vinculado a um lote de compra e não pode ser editado.",
                    HttpStatus.CONFLICT
            );
        }

        order.setCustomerName(request.customerName().trim());
        order.setCustomerPhone(request.customerPhone());
        order.setCustomerDocument(request.customerDocument());
        order.setQuantityKg(request.quantityKg());
        order.setPricePerKg(request.pricePerKg());
        order.setProduct(request.product().trim());
        order.setOrderDate(request.orderDate());
        order.setDeliveryDeadline(request.deliveryDeadline());
        order.setNotes(request.notes());

        return CustomerOrderResponse.from(orderRepository.save(order), CustomerOrderStatus.PENDING);
    }

    @Transactional
    public void delete(UUID accountId, UUID userId, UUID orderId) {
        validateMembership(accountId, userId);

        CustomerOrder order = orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));

        if (lotRepository.existsByCustomerOrderId(orderId)) {
            throw new BusinessException(
                    "Pedido já vinculado a um lote de compra e não pode ser excluído.",
                    HttpStatus.CONFLICT
            );
        }

        orderRepository.delete(order);
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
