package br.com.hadryan.agro.manager.domain.user;

public record UserPreferencesResponse(int notificationDaysAhead) {

    public static UserPreferencesResponse from(UserPreferences prefs) {
        return new UserPreferencesResponse(prefs.getNotificationDaysAhead());
    }
}
