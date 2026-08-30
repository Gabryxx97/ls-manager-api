package it.lsimpresa.manager.auth;

import it.lsimpresa.manager.user.IUserService;
import it.lsimpresa.manager.user.ResetPasswordDto;
import it.lsimpresa.manager.user.UserDto;
import it.lsimpresa.manager.utils.search.SearchControllerBase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController extends SearchControllerBase {

    private final IAuthService service;
    private final IUserService userService;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody AuthDto auth, HttpServletResponse response) {
        service.login(auth, response);
        return response();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        service.refresh(request, response);
        return response();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal ApplicationUserDetails userDetails) {
        UserDto dto = userService.find(userDetails.getId());
        return response(dto);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie acc = ResponseCookie.from("access_token","").path("/").maxAge(0).httpOnly(true).build();
        ResponseCookie ref = ResponseCookie.from("refresh_token","").path("/api/refresh").maxAge(0).httpOnly(true).build();
        response.addHeader(HttpHeaders.SET_COOKIE, acc.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, ref.toString());
        return response();
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signup(@RequestBody UserDto user) {
        return response(service.signup(user));
    }

    @GetMapping("/account-verify")
    public RedirectView verifyAccount(@RequestParam("token") String token) {
       return service.verifyAccount(token);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<UserDto> resetPassword(@RequestBody ResetPasswordDto resetPassword) {
        return response(service.resetPassword(resetPassword));
    }
}
