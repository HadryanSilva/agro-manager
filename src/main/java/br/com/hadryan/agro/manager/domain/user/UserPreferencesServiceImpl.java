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
        userPreferencesRepository.insertDefaultIfNotExists(userId);
        return UserPreferencesResponse.from(
                userPreferencesRepository.findByUserId(userId).orElseThrow()
        );
    }

    @Transactional
    public UserPreferencesResponse update(UUID userId, UserPreferencesRequest request) {
        userPreferencesRepository.insertDefaultIfNotExists(userId);
        UserPreferences prefs = userPreferencesRepository.findByUserId(userId).orElseThrow();
        prefs.setNotificationDaysAhead(request.notificationDaysAhead());
        return UserPreferencesResponse.from(userPreferencesRepository.save(prefs));
    }
}
