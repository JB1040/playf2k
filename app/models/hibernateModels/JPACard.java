package models.hibernateModels;


import play.db.jpa.JPAApi;
import play.db.*;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;

import models.DatabaseExecutionContext;
import models.User;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Provide JPA operations running inside of a thread pool sized to the connection pool
 */
public class JPACard implements CardRepository  {

    private final JPAApi jpaApi;
    private final DatabaseExecutionContext executionContext;
    @Inject
    public JPACard(JPAApi jpaApi, DatabaseExecutionContext executionContext) {
        this.jpaApi = jpaApi;
        this.executionContext = executionContext;
    }

    
    public CompletionStage<Stream<BaseHearthstoneCard>> HSlist() {
        return supplyAsync(() -> wrap(em -> list(em)), executionContext);
    }
    
    public CompletionStage<Stream<BaseGwentCard>> GWlist() {
        return supplyAsync(() -> wrap(em -> listGW(em)), executionContext);
    }
    
    public CompletionStage<BaseCard> insert(BaseCard c) {
        return supplyAsync(() -> wrap(em -> insert(em,c)), executionContext);
    }
    


    private <T> T wrap(Function<EntityManager, T> function) {
        return jpaApi.withTransaction(function);
    }
    



    private BaseCard insert(EntityManager em,BaseCard c) {

    	em.persist(c);
		return c;
      }
    
    private Stream<BaseGwentCard> listGW(EntityManager em) {
      	 TypedQuery<BaseGwentCard> q= em.createQuery("select a from BaseGwentCard a " , BaseGwentCard.class);
      	 	List<BaseGwentCard> art = q.getResultList();
          return art.stream();
      }

    private Stream<BaseHearthstoneCard> list(EntityManager em) {
   	 TypedQuery<BaseHearthstoneCard> q= em.createQuery("select a from BaseHearthstoneCard a " , BaseHearthstoneCard.class);
   	 	List<BaseHearthstoneCard> art = q.getResultList();
       return art.stream();
   }




   
    





}
