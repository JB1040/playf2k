package models;

import com.google.inject.ImplementedBy;

import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

/**
 * This interface provides a non-blocking API for possibly blocking operations.
 */
@ImplementedBy(JPAGwentCard.class)
public interface GwentCardRepository {

    
    CompletionStage<Stream<GwentCard>> list();

}