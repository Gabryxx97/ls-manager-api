package it.lsimpresa.manager.user;

import it.lsimpresa.manager.exception.ConflictException;
import it.lsimpresa.manager.exception.ResourceNotFoundException;
import it.lsimpresa.manager.utils.UserPrincipal;
import jakarta.transaction.Transactional;
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
    @Transactional
    public UserDto create(UserDto user) {
        String username = user.getUsername().trim();
        if (repository.existsByUsername(username)) {
            throw new ConflictException("Esiste già un utente con questo username");
        }

        UserJpa entity = mapper.toJpa(user);
        entity.setId(null);
        entity.setUsername(username);
        entity.setPassword(passwordEncoder.encode(user.getPassword()));
        return mapper.toDto(repository.save(entity));
    }

    @Override
    @Transactional
    public UserDto update(Long id, UserDto user) {
        UserJpa entity = findEntity(id);
        String username = user.getUsername().trim();
        if (repository.existsByUsernameAndIdNot(username, id)) {
            throw new ConflictException("Esiste già un utente con questo username");
        }

        entity.setUsername(username);
        entity.setName(user.getName());
        entity.setSurname(user.getSurname());
        entity.setRole(user.getRole());
        entity.setAccountActive(user.getAccountActive());
        if (user.getPassword() != null) {
            entity.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return mapper.toDto(repository.save(entity));
    }

    @Override
    public UserDto findByUsername(String username) {
        UserJpa user = repository.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("Utente non trovato");
        }
        return mapper.toDto(user);
    }

    @Override
    public UserDto find(Long id) {
        return mapper.toDto(findEntity(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        UserJpa userAuth = userPrincipal.findUserByPrincipal();
        UserJpa user = findEntity(id);
        if (userAuth.getId().equals(user.getId())) {
            throw new ConflictException("Non puoi eliminare il tuo account attivo");
        }
        repository.delete(user);
    }

    private UserJpa findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }
}
