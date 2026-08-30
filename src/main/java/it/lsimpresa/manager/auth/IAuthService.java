package it.lsimpresa.manager.auth;

import it.lsimpresa.manager.user.ResetPasswordDto;
import it.lsimpresa.manager.user.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.view.RedirectView;

public interface IAuthService {
    void login(AuthDto auth, HttpServletResponse response);
    void refresh(HttpServletRequest request, HttpServletResponse response);
    UserDto signup(UserDto user);
    RedirectView verifyAccount(String token);
    UserDto resetPassword(ResetPasswordDto resetPasswordDto);
}
