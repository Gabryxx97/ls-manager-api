package it.lsimpresa.manager.auth;

import it.lsimpresa.manager.user.UserJpa;
import it.lsimpresa.manager.user.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ApplicationUserDetailsService implements UserDetailsService {

    private UserRepository userJpaRepository;

    @Override
    public ApplicationUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserJpa usr = userJpaRepository.findByUsername(username);
        if (usr == null) throw new UsernameNotFoundException("User not found " + username + " ");

        return ApplicationUserDetails.builder()
                        .id(usr.getId())
                        .username(usr.getUsername())
                        .password(usr.getPassword())
                        .role(usr.getRole())
                        .build();
    }
}
