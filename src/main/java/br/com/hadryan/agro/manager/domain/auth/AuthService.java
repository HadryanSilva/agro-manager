package br.com.hadryan.agro.manager.domain.auth;

/** Contrato dos fluxos de autenticação local. */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);
}