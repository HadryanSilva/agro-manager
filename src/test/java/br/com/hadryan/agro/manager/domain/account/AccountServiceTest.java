package br.com.hadryan.agro.manager.domain.account;

import br.com.hadryan.agro.manager.domain.expense.ExpenseRepository;
import br.com.hadryan.agro.manager.domain.farm.FarmActivityRepository;
import br.com.hadryan.agro.manager.domain.farm.FarmRepository;
import br.com.hadryan.agro.manager.domain.labor.EmployeePaymentExpenseRepository;
import br.com.hadryan.agro.manager.domain.labor.EmployeePaymentRepository;
import br.com.hadryan.agro.manager.domain.labor.EmployeeRepository;
import br.com.hadryan.agro.manager.domain.labor.EmployeeWorkEntryRepository;
import br.com.hadryan.agro.manager.domain.quotation.QuotationRepository;
import br.com.hadryan.agro.manager.domain.trading.ClientOrderRepository;
import br.com.hadryan.agro.manager.domain.trading.OrderSupplierLegRepository;
import br.com.hadryan.agro.manager.domain.trading.OrderTruckRepository;
import br.com.hadryan.agro.manager.domain.trading.TradingClientRepository;
import br.com.hadryan.agro.manager.domain.trading.TradingSupplierRepository;
import br.com.hadryan.agro.manager.domain.user.User;
import br.com.hadryan.agro.manager.domain.user.UserRepository;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de AccountServiceImpl.
 * Isola o serviço via mocks — sem Spring context ou banco de dados.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService — Gerenciamento de contas")
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private AccountMemberRepository accountMemberRepository;
    @Mock private AccountInviteRepository accountInviteRepository;
    @Mock private OrderTruckRepository orderTruckRepository;
    @Mock private OrderSupplierLegRepository orderSupplierLegRepository;
    @Mock private ClientOrderRepository clientOrderRepository;
    @Mock private FarmActivityRepository farmActivityRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private QuotationRepository quotationRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private TradingClientRepository tradingClientRepository;
    @Mock private TradingSupplierRepository tradingSupplierRepository;
    @Mock private EmployeePaymentExpenseRepository employeePaymentExpenseRepository;
    @Mock private EmployeeWorkEntryRepository employeeWorkEntryRepository;
    @Mock private EmployeePaymentRepository employeePaymentRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private AccountServiceImpl accountService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private User buildUser(UUID id) {
        return User.builder()
                .id(id)
                .name("Usuário Teste")
                .email("user@test.com")
                .build();
    }

    private Account buildAccount(UUID id, String name, User owner) {
        Account account = Account.builder()
                .name(name)
                .owner(owner)
                .build();
        // Simula o ID gerado pelo banco
        try {
            var field = Account.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(account, id);
            var createdAt = Account.class.getDeclaredField("createdAt");
            createdAt.setAccessible(true);
            createdAt.set(account, LocalDateTime.now());
        } catch (Exception ignored) {}
        return account;
    }

    // ── createAccount ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve criar conta e retornar AccountResponse com papel OWNER")
    void createAccount_success() {
        UUID userId = UUID.randomUUID();
        User owner = buildUser(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.createAccount(
                userId, new CreateAccountRequest("Fazenda Teste"));

        assertThat(response.name()).isEqualTo("Fazenda Teste");
        assertThat(response.userRole()).isEqualTo(AccountRole.OWNER);
        assertThat(response.memberCount()).isEqualTo(1L);
        verify(accountRepository).save(any(Account.class));
        verify(accountMemberRepository).save(any(AccountMember.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando usuário não existe")
    void createAccount_userNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                accountService.createAccount(userId, new CreateAccountRequest("Fazenda")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(accountRepository, never()).save(any());
    }

    // ── getUserAccounts ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve retornar lista vazia quando usuário não tem contas")
    void getUserAccounts_empty() {
        UUID userId = UUID.randomUUID();
        when(accountMemberRepository.findByUserId(userId)).thenReturn(List.of());

        List<AccountResponse> result = accountService.getUserAccounts(userId);

        assertThat(result).isEmpty();
        verify(accountMemberRepository, never()).countMembersByAccountIds(any());
    }

    @Test
    @DisplayName("Deve buscar contagem em lote sem N+1 quando usuário tem múltiplas contas")
    void getUserAccounts_batchCount_noNPlusOne() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);

        UUID accountId1 = UUID.randomUUID();
        UUID accountId2 = UUID.randomUUID();
        Account account1 = buildAccount(accountId1, "Conta 1", user);
        Account account2 = buildAccount(accountId2, "Conta 2", user);

        AccountMember member1 = AccountMember.builder()
                .account(account1).user(user).role(AccountRole.OWNER).build();
        AccountMember member2 = AccountMember.builder()
                .account(account2).user(user).role(AccountRole.ADMIN).build();

        when(accountMemberRepository.findByUserId(userId))
                .thenReturn(List.of(member1, member2));
        when(accountMemberRepository.countMembersByAccountIds(any()))
                .thenReturn(List.of(
                        new Object[]{accountId1, 3L},
                        new Object[]{accountId2, 1L}
                ));

        List<AccountResponse> result = accountService.getUserAccounts(userId);

        assertThat(result).hasSize(2);
        // Verifica que apenas UMA query de contagem foi feita (sem N+1)
        verify(accountMemberRepository, times(1)).countMembersByAccountIds(any());
    }

    // ── deleteAccount ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve excluir conta quando OWNER confirma com nome correto")
    void deleteAccount_success() {
        UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User owner = buildUser(userId);
        Account account = buildAccount(accountId, "Fazenda Real", owner);

        AccountMember ownerMember = AccountMember.builder()
                .account(account).user(owner).role(AccountRole.OWNER).build();

        when(accountMemberRepository.findByAccountIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(ownerMember));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        accountService.deleteAccount(accountId, userId,
                new DeleteAccountRequest("Fazenda Real"));

        verify(accountRepository).delete(account);
        verify(employeePaymentExpenseRepository).deleteByPaymentAccountId(accountId);
        verify(employeeWorkEntryRepository).deleteByAccountId(accountId);
        verify(employeePaymentRepository).deleteByAccountId(accountId);
        verify(employeeRepository).deleteByAccountId(accountId);
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando MEMBER tenta excluir a conta")
    void deleteAccount_memberCannotDelete() {
        UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        Account account = buildAccount(accountId, "Conta", user);

        AccountMember member = AccountMember.builder()
                .account(account).user(user).role(AccountRole.MEMBER).build();

        when(accountMemberRepository.findByAccountIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(member));

        assertThatThrownBy(() ->
                accountService.deleteAccount(accountId, userId,
                        new DeleteAccountRequest("Conta")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OWNER");

        verify(accountRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando nome de confirmação não confere")
    void deleteAccount_wrongConfirmationName() {
        UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User owner = buildUser(userId);
        Account account = buildAccount(accountId, "Nome Real", owner);

        AccountMember ownerMember = AccountMember.builder()
                .account(account).user(owner).role(AccountRole.OWNER).build();

        when(accountMemberRepository.findByAccountIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(ownerMember));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() ->
                accountService.deleteAccount(accountId, userId,
                        new DeleteAccountRequest("Nome Errado")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não confere");

        verify(accountRepository, never()).delete(any());
    }
}
