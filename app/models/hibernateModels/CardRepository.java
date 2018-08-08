package models.hibernateModels;

import com.google.inject.ImplementedBy;

import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

/**
 * This interface provides a non-blocking API for possibly blocking operations.
 */
@ImplementedBy(JPACard.class)
public interface CardRepository {


    CompletionStage<Stream<BaseHearthstoneCard>> HSlist();
    CompletionStage<Stream<BaseGwentCard>> GWlist();
    public CompletionStage<BaseCard> insert(BaseCard c);
}