package it.lsimpresa.manager.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserJpa, Long>{
	UserJpa findByUsername(String username);
	UserJpa findByTokenActive(String token);
}
