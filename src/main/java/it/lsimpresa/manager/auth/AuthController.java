package it.lsimpresa.manager.auth;

import it.lsimpresa.manager.user.IUserService;
import it.lsimpresa.manager.user.UserDto;
import it.lsimpresa.manager.utils.search.SearchControllerBase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController extends SearchControllerBase {

    private final IAuthService service;
    private final IUserService userService;
    private final CsrfTokenRepository csrfTokenRepository;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody AuthDto auth, HttpServletResponse response) {
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
        service.logout(response);
        return response();
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(csrfToken, request, response);
        return ResponseEntity.noContent().build();
    }
}
