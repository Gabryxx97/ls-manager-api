package it.lsimpresa.manager.user;

public interface IUserService {
    UserDto save(UserDto user);
    UserDto findByUsername(String username);
    UserDto find(Long id);
    UserDto delete(Long	id);
}
