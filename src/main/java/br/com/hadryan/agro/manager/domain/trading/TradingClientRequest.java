package br.com.hadryan.agro.manager.domain.trading;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TradingClientRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String name,

        @NotBlank(message = "Telefone é obrigatório")
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String phone,

        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
        String city,

        String notes
) {
}
