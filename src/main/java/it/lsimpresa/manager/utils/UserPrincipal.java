package it.lsimpresa.manager.utils;

import it.lsimpresa.manager.user.UserJpa;
import it.lsimpresa.manager.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserPrincipal {

    private final UserRepository userRepository;

    public UserJpa findUserByPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserJpa user = userRepository.findByUsername(email);
        if(user == null) {
            throw new RuntimeException("Utente non trovato");
        }
        return user;
    }
}
