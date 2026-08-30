package it.lsimpresa.manager.user;

import lombok.Data;

public @Data class ResetPasswordDto {
	private String password;
	private String token;
	private String username;
}
