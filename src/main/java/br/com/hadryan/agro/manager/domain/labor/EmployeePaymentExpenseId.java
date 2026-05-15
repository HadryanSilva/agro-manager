package br.com.hadryan.agro.manager.domain.labor;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EmployeePaymentExpenseId implements Serializable {
    private UUID paymentId;
    private UUID expenseId;
}
