package br.com.hadryan.agro.manager.domain.user;

import java.util.UUID;

/** Contrato do serviço de perfil de usuário. */
public interface UserProfileService {

    UserProfileResponse getProfile(UUID userId);

    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);
}