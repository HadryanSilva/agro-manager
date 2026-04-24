package br.com.hadryan.agro.manager.domain.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados necessários para criar uma nova conta.
 */
public record CreateAccountRequest(

        @NotBlank(message = "Nome da conta é obrigatório")
        @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
        String name
) {}