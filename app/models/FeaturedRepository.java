package models;

import com.google.inject.ImplementedBy;

import models.hibernateModels.BaseArticle;

import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

/**
 * This interface provides a non-blocking API for possibly blocking operations.
 */
@ImplementedBy(JPAFeatured.class)
public interface FeaturedRepository {

    
    CompletionStage<Stream<ArticleFeatured>> list();

    CompletionStage<ArticleFeatured> editID(long id);
    CompletionStage<ArticleFeatured> editArticle(BaseArticle id);
    CompletionStage<ArticleFeatured> get(long id);
}