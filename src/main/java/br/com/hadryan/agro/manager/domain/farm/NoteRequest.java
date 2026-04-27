package br.com.hadryan.agro.manager.domain.farm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload para criação de anotação manual no histórico da lavoura.
 */
public record NoteRequest(
        @NotBlank(message = "A anotação não pode estar vazia")
        @Size(max = 1000, message = "Anotação deve ter no máximo 1000 caracteres")
        String description
) {}