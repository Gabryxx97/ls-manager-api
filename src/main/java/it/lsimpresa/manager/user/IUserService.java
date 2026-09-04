package it.lsimpresa.manager.user;

public interface IUserService {
    UserDto create(UserDto user);
    UserDto update(Long id, UserDto user);
    UserDto findByUsername(String username);
    UserDto find(Long id);
    void delete(Long id);
}
