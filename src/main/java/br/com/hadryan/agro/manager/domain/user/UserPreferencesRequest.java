package br.com.hadryan.agro.manager.domain.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UserPreferencesRequest(
        @NotNull(message = "notificationDaysAhead é obrigatório")
        @Min(value = 1, message = "notificationDaysAhead deve ser pelo menos 1")
        @Max(value = 90, message = "notificationDaysAhead não pode ser maior que 90")
        Integer notificationDaysAhead
) {}
