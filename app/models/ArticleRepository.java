package models;

import com.google.inject.ImplementedBy;

import models.Article.ArtType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

/**
 * This interface provides a non-blocking API for possibly blocking operations.
 */
@ImplementedBy(JPAArticle.class)
public interface ArticleRepository {

    CompletionStage<Article> add(Article art);
    
    CompletionStage<Article> get(long id);

    CompletionStage<Article> edit(Article art);
    CompletionStage<Article> upvote(long id);
    
    CompletionStage<Stream<Article>> list(int offset,int amount,ArtType type);

    CompletionStage<Stream<Article>> byAuthor(long id);

}