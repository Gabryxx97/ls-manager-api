package it.lsimpresa.manager.auth;

import it.lsimpresa.manager.user.UserDto;
import lombok.Data;

public @Data class JwtToken {
	private String accessToken;
	private UserDto user;
}
