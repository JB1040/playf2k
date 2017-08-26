package models;


import play.db.jpa.JPAApi;
import play.db.*;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Provide JPA operations running inside of a thread pool sized to the connection pool
 */
public class JPAFeatured implements FeaturedRepository  {

    private final JPAApi jpaApi;
    private final DatabaseExecutionContext executionContext;
    @Inject
    public JPAFeatured(JPAApi jpaApi, DatabaseExecutionContext executionContext) {
        this.jpaApi = jpaApi;
        this.executionContext = executionContext;
    }

    
    public CompletionStage<Stream<ArticleFeatured>> list() {
        return supplyAsync(() -> wrap(em -> list(em)), executionContext);
    }
    


    private <T> T wrap(Function<EntityManager, T> function) {
        return jpaApi.withTransaction(function);
    }
    


	

    private Stream<ArticleFeatured> list(EntityManager em) {
   	 TypedQuery<ArticleFeatured> q= em.createQuery("select a from ArticleFeatured a " , ArticleFeatured.class);
   	 	List<ArticleFeatured> art = q.getResultList();
       return art.stream();
   }




   
    





}
