package br.com.hadryan.agro.manager.domain.account;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload para exclusão de conta.
 * O usuário deve confirmar digitando o nome exato da conta
 * — validação duplicada no backend como camada extra de segurança.
 */
public record DeleteAccountRequest(

        @NotBlank(message = "O nome de confirmação é obrigatório")
        String confirmationName
) {}