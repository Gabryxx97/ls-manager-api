package it.lsimpresa.manager.user;

import it.lsimpresa.manager.exception.ResourceNotAccessExeption;
import it.lsimpresa.manager.utils.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final UserPrincipal userPrincipal;

    @Override
    public UserDto save(UserDto user) {
        UserJpa userAuth = userPrincipal.findUserByPrincipal();
        if(!userAuth.getRole().equals(UserRole.ADMIN_ROLE)) {
            throw new ResourceNotAccessExeption("Non hai i permessi per creare/modificare utenti");
        }
        UserJpa entity = mapper.toJpa(user);
        if(user.getId() != null) {
            UserJpa existingUser = repository.findByUsername(user.getUsername());
            if(user.getPassword() != null) {
                entity.setPassword(passwordEncoder.encode(user.getPassword()));
            } else {
                entity.setPassword(existingUser.getPassword());
            }
        }
        return mapper.toDto(repository.save(entity));
    }

    @Override
    public UserDto findByUsername(String username) {
        UserJpa user = repository.findByUsername(username);
        return mapper.toDto(user);
    }

    @Override
    public UserDto find(Long id) {
        UserJpa user = repository.findById(id).orElse(null);
        return mapper.toDto(user);
    }

    @Override
    public UserDto delete(Long id) {
        UserJpa userAuth = userPrincipal.findUserByPrincipal();
        if(!userAuth.getRole().equals(UserRole.ADMIN_ROLE)) {
            throw new ResourceNotAccessExeption("Non hai i permessi per eliminare utenti");
        }
        UserJpa user = repository.findById(id).orElse(null);
        if (user != null) {
            repository.delete(user);
        }
        return mapper.toDto(user);
    }
}
