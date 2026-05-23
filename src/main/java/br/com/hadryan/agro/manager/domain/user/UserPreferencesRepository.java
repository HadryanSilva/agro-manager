package br.com.hadryan.agro.manager.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {
    Optional<UserPreferences> findByUserId(UUID userId);

    @Modifying
    @Query(value = "INSERT INTO user_preferences (id, user_id, notification_days_ahead, created_at, updated_at) " +
                   "VALUES (gen_random_uuid(), :userId, 7, NOW(), NOW()) " +
                   "ON CONFLICT (user_id) DO NOTHING",
           nativeQuery = true)
    void insertDefaultIfNotExists(@Param("userId") UUID userId);
}
