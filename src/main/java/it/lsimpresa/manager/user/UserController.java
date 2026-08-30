package it.lsimpresa.manager.user;

import it.lsimpresa.manager.utils.search.ISearchService;
import it.lsimpresa.manager.utils.search.SearchControllerBase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/user")
public class UserController extends SearchControllerBase {

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
                filter(fils),
                sort(sor),
                pageable(page,size),
                UserJpa.class, userMapper);
        return response(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> get(@PathVariable("id") Long id) {
        return response(userService.find(id));
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<UserDto> getProfile(@PathVariable("id") Long id) {
        return response(userService.find(id));
    }

    @PostMapping
    public ResponseEntity<UserDto> save(@RequestBody UserDto user) {
        return response(userService.save(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UserDto> delete(@PathVariable("id") Long id) {
        return response(userService.delete(id));
    }

}

