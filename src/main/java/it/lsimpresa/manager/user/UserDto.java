package it.lsimpresa.manager.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

public @Data class UserDto {
	private Long id;
	@NotBlank(groups = {UserValidation.Create.class, UserValidation.Update.class},
			message = "Lo username è obbligatorio")
	@Size(max = 128, groups = {UserValidation.Create.class, UserValidation.Update.class},
			message = "Lo username non può superare 128 caratteri")
	private String username;

    @NotNull(groups = {UserValidation.Create.class, UserValidation.Update.class},
			message = "Il ruolo è obbligatorio")
    private UserRole role;
	@Size(max = 128, groups = {UserValidation.Create.class, UserValidation.Update.class},
			message = "Il nome non può superare 128 caratteri")
	private String name;
	@Size(max = 128, groups = {UserValidation.Create.class, UserValidation.Update.class},
			message = "Il cognome non può superare 128 caratteri")
	private String surname;
	@NotBlank(groups = UserValidation.Create.class, message = "La password è obbligatoria")
	@Pattern(regexp = ".*\\S.*", groups = UserValidation.Update.class,
			message = "La password non può essere vuota")
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@ToString.Exclude
	private String password;
	private Boolean accountActive = false;
}
