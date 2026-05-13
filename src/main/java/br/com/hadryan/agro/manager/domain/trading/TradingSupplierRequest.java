package br.com.hadryan.agro.manager.domain.trading;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados para criação ou atualização de um fornecedor.
 */
public record TradingSupplierRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String name,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String phone,

        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
        String city,

        String notes
) {
}
