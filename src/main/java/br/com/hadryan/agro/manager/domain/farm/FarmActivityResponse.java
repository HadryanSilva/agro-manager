package br.com.hadryan.agro.manager.domain.farm;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta de uma atividade do histórico da lavoura.
 */
public record FarmActivityResponse(
        UUID id,
        FarmActivityType type,
        String description,
        UUID relatedId,
        String userName,
        String userAvatarUrl,
        LocalDateTime createdAt
) {
    public static FarmActivityResponse from(FarmActivity activity) {
        return new FarmActivityResponse(
                activity.getId(),
                activity.getType(),
                activity.getDescription(),
                activity.getRelatedId(),
                activity.getUser().getName(),
                activity.getUser().getAvatarUrl(),
                activity.getCreatedAt()
        );
    }
}