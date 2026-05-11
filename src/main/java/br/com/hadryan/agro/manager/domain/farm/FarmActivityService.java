package br.com.hadryan.agro.manager.domain.farm;

import java.util.List;
import java.util.UUID;

/** Contrato do serviço de atividades de lavoura. */
public interface FarmActivityService {

    List<FarmActivityResponse> getActivities(UUID accountId, UUID farmId, UUID userId);

    FarmActivityResponse addNote(UUID accountId, UUID farmId, UUID userId, NoteRequest request);

    void record(UUID farmId, UUID userId, FarmActivityType type, String description, UUID relatedId);
}