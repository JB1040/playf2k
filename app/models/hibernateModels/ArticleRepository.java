package models.hibernateModels;

import com.google.inject.ImplementedBy;

import enums.General.Game;
import enums.General.TextType;
import enums.Hearthstone.Hero;
import enums.Hearthstone.Mode;
import enums.General.ArtType;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;


/**
 * This interface provides a non-blocking API for possibly blocking operations.
 */
@ImplementedBy(JPAArticle.class)
public interface ArticleRepository {

    CompletionStage<BaseArticle> add(BaseArticle art,List<Long> recommended,List<Long> similar);
    
    CompletionStage<BaseArticle> get(long id);

    CompletionStage<BaseArticle> edit(BaseArticle art,List<Long> recommended,List<Long> similar);
    CompletionStage<BaseArticle> upvote(long id);
    
    CompletionStage<Stream<BaseArticle>> list(int offset,int amount,ArtType type,TextType textType,Game game,int tier,Mode mode,Boolean isStandard);


    CompletionStage<Stream<BaseArticle>> list(int offset,int amount,ArtType type,TextType textType,Game game,int tier,Mode mode,Boolean isStandard,boolean forceAll);
    
    CompletionStage<Stream<Object>> tierlist(int offset,int amount,Game game,int rankMin,int rankMax,Mode mode,Boolean isStandard,Hero[] heroes);

    CompletionStage<Stream<BaseArticle>> byAuthor(long id);

}