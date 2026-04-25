package br.com.hadryan.agro.manager.domain.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload para atualização do nome do usuário.
 * E-mail não é alterável por esta rota por questões de segurança.
 */
public record UpdateProfileRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String name
) {}