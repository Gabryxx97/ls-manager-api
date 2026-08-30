package it.lsimpresa.manager.user;

import it.lsimpresa.manager.utils.DtoMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.stream.Collectors;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public class UserMapper implements DtoMapper<UserDto, UserJpa> {

	public UserDto toDto(UserJpa jpa) {
		if (jpa == null) return null;

		UserDto dto = new UserDto();
		dto.setId(jpa.getId());
		dto.setUsername(jpa.getUsername());
		dto.setName(jpa.getName());
		dto.setSurname(jpa.getSurname());
		dto.setAccountActive(jpa.getAccountActive());
		dto.setRole(jpa.getRole());

		return dto;
	}

	public UserJpa toJpa(UserDto dto) {
		if (dto == null) return null;

		UserJpa entity = new UserJpa();
		entity.setId(dto.getId());
		entity.setUsername(dto.getUsername());
		entity.setName(dto.getName());
		entity.setSurname(dto.getSurname());
		entity.setAccountActive(dto.getAccountActive());
		entity.setRole(dto.getRole());
		// Nota: non impostiamo relazioni per evitare ricorsione o errori
		return entity;
	}

	@Override
	public List<UserDto> map(List<UserJpa> users) {
		return users.stream().map(this::toDto).collect(Collectors.toList());
	}

	public UserDto toDtoWithRelations(UserJpa user) {
		UserDto dto = toDto(user); // chiama il mapping base

		if (user == null) return null;

		return dto;
	}

	public List<UserDto> mapWithRelations(List<UserJpa> users) {
		return users.stream()
				.map(this::toDtoWithRelations)
				.collect(Collectors.toList());
	}
}



