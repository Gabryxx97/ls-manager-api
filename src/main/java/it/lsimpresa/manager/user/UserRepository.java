package it.lsimpresa.manager.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserJpa, Long>{
	UserJpa findByUsername(String username);
	boolean existsByUsername(String username);
	boolean existsByUsernameAndIdNot(String username, Long id);
	UserJpa findByTokenActive(String token);
}
