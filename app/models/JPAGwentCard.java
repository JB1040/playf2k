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
public class JPAGwentCard implements GwentCardRepository  {

    private final JPAApi jpaApi;
    private final DatabaseExecutionContext executionContext;
    @Inject
    public JPAGwentCard(JPAApi jpaApi, DatabaseExecutionContext executionContext) {
        this.jpaApi = jpaApi;
        this.executionContext = executionContext;
    }

    
    public CompletionStage<Stream<GwentCard>> list() {
        return supplyAsync(() -> wrap(em -> list(em)), executionContext);
    }
    


    private <T> T wrap(Function<EntityManager, T> function) {
        return jpaApi.withTransaction(function);
    }
    


    public Article edit(EntityManager em, Article art) {
		art.author = em.find(User.class, art.author.id);
		art = em.merge(art);
		em.refresh(art);
		return art;
	}

	

    private Stream<GwentCard> list(EntityManager em) {
   	 TypedQuery<GwentCard> q= em.createQuery("select a from GwentCard a" , GwentCard.class);
   	 	List<GwentCard> art = q.getResultList();
       return art.stream();
   }




   
    





}
