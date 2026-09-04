package it.lsimpresa.manager.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface IAuthService {
    void login(AuthDto auth, HttpServletResponse response);
    void refresh(HttpServletRequest request, HttpServletResponse response);
    void logout(HttpServletResponse response);
}
