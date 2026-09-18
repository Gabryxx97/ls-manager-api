package it.lsimpresa.manager.article;

import it.lsimpresa.manager.utils.search.ISearchService;
import it.lsimpresa.manager.utils.search.SearchControllerBase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
@PreAuthorize("hasAnyAuthority('ADMIN_ROLE', 'USER_ROLE')")
public class ArticleController extends SearchControllerBase {

    private static final Map<String, Set<String>> ALLOWED_FILTERS = Map.of(
            "id", Set.of("EQUAL", "IN"),
            "search", Set.of("ILIKE"));
    private static final Set<String> ALLOWED_SORTS = Set.of(
            "id", "sku", "description", "category", "costCenter", "unitOfMeasure",
            "location", "stockQuantity", "unitPrice");

    private final ISearchService searchService;
    private final ArticleMapper articleMapper;
    private final IArticleService articleService;

    @GetMapping
    public ResponseEntity<List<ArticleDto>> search(
            @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort) {
        Page<ArticleDto> results = searchService.search(
                filter(filter, ALLOWED_FILTERS),
                sort(sort, ALLOWED_SORTS),
                pageable(page, pageSize),
                ArticleJpa.class,
                articleMapper);
        return response(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.find(id));
    }

    @PostMapping
    public ResponseEntity<ArticleDto> create(@Valid @RequestBody ArticleDto article) {
        ArticleDto created = articleService.create(article);
        return ResponseEntity.created(URI.create("/api/articles/" + created.getId())).body(created);
    }

    @PostMapping("/import")
    public ResponseEntity<ArticleImportDto.Response> importArticles(
            @Valid @RequestBody ArticleImportDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(articleService.importArticles(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArticleDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ArticleDto article) {
        return ResponseEntity.ok(articleService.update(id, article));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        articleService.delete(id);
    }
}
