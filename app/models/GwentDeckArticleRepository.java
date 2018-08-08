package models;

import com.google.inject.ImplementedBy;

import models.Article.Game;

import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

/**
 * This interface provides a non-blocking API for possibly blocking operations.
 */
@ImplementedBy(JPAGwentDeckArticle.class)
public interface GwentDeckArticleRepository {

    CompletionStage<DeckGwentArticle> add(DeckGwentArticle art);
    
    CompletionStage<DeckGwentArticle> get(long id);

    CompletionStage<DeckGwentArticle> edit(DeckGwentArticle art);
    CompletionStage<DeckGwentArticle> upvote(long id);
    
    CompletionStage<Stream<DeckGwentArticle>> list(int offset,int amount,int tier,
    			GwentCard.Faction faction, GwentCard.Leader leader,Game game);

    CompletionStage<Stream<DeckGwentArticle>> byAuthor(long id);
}