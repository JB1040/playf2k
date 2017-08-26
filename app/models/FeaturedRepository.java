package models;

import com.google.inject.ImplementedBy;

import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

/**
 * This interface provides a non-blocking API for possibly blocking operations.
 */
@ImplementedBy(JPAFeatured.class)
public interface FeaturedRepository {

    
    CompletionStage<Stream<ArticleFeatured>> list();

}