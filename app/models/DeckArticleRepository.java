package models;

import com.google.inject.ImplementedBy;

import models.Article.Game;

import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

/**
 * This interface provides a non-blocking API for possibly blocking operations.
 */
@ImplementedBy(JPADeckArticle.class)
public interface DeckArticleRepository {

    CompletionStage<DeckArticle> add(DeckArticle art);
    
    CompletionStage<DeckArticle> get(long id);

    CompletionStage<DeckArticle> edit(DeckArticle art);
    CompletionStage<DeckArticle> upvote(long id);
    
    CompletionStage<Stream<DeckArticle>> list(int offset,int amount,int tier,DeckArticle.Mode mode,Boolean isStandard,Game game);

    CompletionStage<Stream<DeckArticle>> byAuthor(long id);
}