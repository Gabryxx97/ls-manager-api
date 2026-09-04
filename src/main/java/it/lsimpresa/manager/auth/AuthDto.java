package it.lsimpresa.manager.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public @Data class AuthDto {
	
	@NotBlank(message = "Lo username è obbligatorio")
	private String username;

	@NotBlank(message = "La password è obbligatoria")
	private String password;
}
