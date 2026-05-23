package br.com.hadryan.agro.manager.domain.user;

import java.util.UUID;

public interface UserPreferencesService {
    UserPreferencesResponse getOrCreate(UUID userId);
    UserPreferencesResponse update(UUID userId, UserPreferencesRequest request);
}
