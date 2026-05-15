package br.com.hadryan.agro.manager.domain.labor;

import br.com.hadryan.agro.manager.domain.expense.Expense;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employee_payment_expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePaymentExpense {

    @EmbeddedId
    private EmployeePaymentExpenseId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("paymentId")
    @JoinColumn(name = "payment_id", nullable = false)
    private EmployeePayment payment;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId("expenseId")
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;
}
