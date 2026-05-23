package br.com.hadryan.agro.manager.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserPreferencesServiceImpl implements UserPreferencesService {

    private final UserPreferencesRepository userPreferencesRepository;

    @Transactional
    public UserPreferencesResponse getOrCreate(UUID userId) {
        return UserPreferencesResponse.from(
                userPreferencesRepository.findByUserId(userId)
                        .orElseGet(() -> createDefault(userId))
        );
    }

    @Transactional
    public UserPreferencesResponse update(UUID userId, UserPreferencesRequest request) {
        UserPreferences prefs = userPreferencesRepository.findByUserId(userId)
                .orElseGet(() -> createDefault(userId));
        prefs.setNotificationDaysAhead(request.notificationDaysAhead());
        return UserPreferencesResponse.from(userPreferencesRepository.save(prefs));
    }

    private UserPreferences createDefault(UUID userId) {
        return userPreferencesRepository.save(
                UserPreferences.builder()
                        .userId(userId)
                        .notificationDaysAhead(7)
                        .build()
        );
    }
}
