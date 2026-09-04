package it.lsimpresa.manager.user;

import it.lsimpresa.manager.utils.search.ISearchService;
import it.lsimpresa.manager.utils.search.SearchControllerBase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAuthority('ADMIN_ROLE')")
public class UserController extends SearchControllerBase {

    private static final Map<String, Set<String>> ALLOWED_FILTERS = Map.of(
            "id", Set.of("EQUAL", "IN"),
            "search", Set.of("ILIKE"),
            "username", Set.of("EQUAL"),
            "role", Set.of("EQUAL"),
            "accountActive", Set.of("EQUAL"));
    private static final Set<String> ALLOWED_SORTS = Set.of(
            "id", "username", "name", "surname", "role", "accountActive");

    private final ISearchService searchService;
    private final UserMapper userMapper;
    private final IUserService userService;

    @GetMapping
    public ResponseEntity<List<UserDto>> search(
            @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer size,
            @RequestParam(value="filter",required = false) String fils,
            @RequestParam(value="sort", required = false) String sor){
        Page<UserDto> results = searchService.<UserDto, UserJpa>search(
                filter(fils, ALLOWED_FILTERS),
                sort(sor, ALLOWED_SORTS),
                pageable(page,size),
                UserJpa.class, userMapper);
        return response(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> get(@PathVariable("id") Long id) {
        return response(userService.find(id));
    }

    @PostMapping
    public ResponseEntity<UserDto> create(
            @Validated(UserValidation.Create.class) @RequestBody UserDto user) {
        UserDto created = userService.create(user);
        return ResponseEntity.created(URI.create("/api/users/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> update(
            @PathVariable("id") Long id,
            @Validated(UserValidation.Update.class) @RequestBody UserDto user) {
        return ResponseEntity.ok(userService.update(id, user));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        userService.delete(id);
    }

}
