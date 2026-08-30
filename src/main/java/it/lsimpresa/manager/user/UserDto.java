package it.lsimpresa.manager.user;

import lombok.Data;

public @Data class UserDto {
	private Long id;
	private String username;
    private UserRole role;
	private String name;
	private String surname;
	private String password;
	private Boolean accountActive = false;
}
