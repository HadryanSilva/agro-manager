package br.com.hadryan.agro.manager.domain.farm;

import java.util.List;
import java.util.UUID;

/** Contrato do serviço de gerenciamento de lavouras. */
public interface FarmService {

    FarmResponse create(UUID accountId, UUID userId, FarmRequest request);

    List<FarmResponse> findAll(UUID accountId, UUID userId, FarmStatus statusFilter);

    FarmResponse findById(UUID accountId, UUID userId, UUID farmId);

    FarmResponse update(UUID accountId, UUID userId, UUID farmId, FarmRequest request);

    void delete(UUID accountId, UUID userId, UUID farmId);
}