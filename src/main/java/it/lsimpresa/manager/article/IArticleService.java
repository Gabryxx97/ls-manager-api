package it.lsimpresa.manager.article;

public interface IArticleService {

    ArticleDto create(ArticleDto article);

    ArticleDto update(Long id, ArticleDto article);

    ArticleDto find(Long id);

    ArticleImportDto.Response importArticles(ArticleImportDto.Request request);

    void delete(Long id);
}
