package models;


import play.db.jpa.JPAApi;
import play.db.*;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;

import models.hibernateModels.BaseArticle;
import models.hibernateModels.TextArticle;

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
    
	@Override
	public CompletionStage<ArticleFeatured> get(long id) {
		  return supplyAsync(() -> wrap(em -> getByID(em,id)), executionContext);
	}


	@Override
	public CompletionStage<ArticleFeatured> editID(long newId) {
		return supplyAsync(() -> wrap(em -> editID(em,newId)),executionContext);
	}

	
	@Override
	public CompletionStage<ArticleFeatured> editArticle(BaseArticle newId) {
		return supplyAsync(() -> wrap(em -> editArticle(em,newId)),executionContext);
	}


    private ArticleFeatured editArticle(EntityManager em, BaseArticle newId) {
		ArticleFeatured art = getByID(em, 1);
		art.article = newId;
		return em.merge(art);
	}


	private <T> T wrap(Function<EntityManager, T> function) {
        return jpaApi.withTransaction(function);
    }
    


	

    private Stream<ArticleFeatured> list(EntityManager em) {
   	 TypedQuery<ArticleFeatured> q= em.createQuery("select a from ArticleFeatured a " , ArticleFeatured.class);
   	 	List<ArticleFeatured> art = q.getResultList();
       return art.stream();
   }

    
    private ArticleFeatured getByID(EntityManager em,long id) {
		ArticleFeatured result = em.find(ArticleFeatured.class, id);
       return result;
    }
    
    private ArticleFeatured editID(EntityManager em, long newId) {
		ArticleFeatured art = getByID(em, 1);
		art.article = (BaseArticle) em.find(BaseArticle.class,newId);
		return em.merge(art);
	}



   
    





}
