package it.lsimpresa.manager.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.lsimpresa.manager.utils.search.Searchable;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_user")
public @Data class UserJpa implements Searchable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="id")
	private Long id;

	@Column(name = "username", unique=true,length = 128)
    private String username;

	@Column(name = "password")
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String password;

	@Column(name = "name",length = 128)
	private String name;

	@Column(name = "surname",length = 128)
	private String surname;

	@Column(name = "role")
	@Enumerated(EnumType.STRING)
    private UserRole role;

	@Column(name = "account_active", columnDefinition = "boolean default false")
	private Boolean accountActive = false;

	@Column(name="token_active", columnDefinition = "text")
	private String tokenActive;

	@Column(name="token_active_date")
	private LocalDateTime tokenActiveDate;

	@Column(name="token_password")
	private String tokenPassword;

	@Column(name="token_validity")
	private LocalDateTime tokenValidity;

	@Column(name="token_use")
	private Boolean tokenUse;

	@Column(name="search", columnDefinition = "text")
	private String search;

	@PreUpdate
	@PrePersist
	public void searchFill() {
		search = name + " " + surname + " " + username;
	}
}
