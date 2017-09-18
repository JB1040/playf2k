package models;


import play.db.jpa.JPAApi;
import play.db.*;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;

import models.Article.ArtType;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Provide JPA operations running inside of a thread pool sized to the connection pool
 */
public class JPAArticle implements ArticleRepository  {

    private final JPAApi jpaApi;
    private final DatabaseExecutionContext executionContext;
    private static final String BYDATE = " ORDER BY date DESC";
    private static final String HSONLY = " game = 'HS' ";

    @Inject
    public JPAArticle(JPAApi jpaApi, DatabaseExecutionContext executionContext) {
        this.jpaApi = jpaApi;
        this.executionContext = executionContext;
    }

    public CompletionStage<Article> add(Article art) {
        return supplyAsync(() -> wrap(em -> insert(em, art)), executionContext);
    }
    
	@Override
	public CompletionStage<Article> get(long id) {
		  return supplyAsync(() -> wrap(em -> getByID(em,id)), executionContext);
	}

    public CompletionStage<Stream<Article>> list(int offset,int amount,ArtType type) {
        return supplyAsync(() -> wrap(em -> list(em,offset,amount,type)), executionContext);
    }
    

	@Override
	public CompletionStage<Stream<Article>> byAuthor(long id) {
		return supplyAsync(() -> wrap(em -> listByCreator(em,id)), executionContext);
	}



	@Override
	public CompletionStage<Article> edit(Article art) {
		return supplyAsync(() -> wrap(em -> edit(em,art)),executionContext);
	}

    private <T> T wrap(Function<EntityManager, T> function) {
        return jpaApi.withTransaction(function);
    }
    


    public Article edit(EntityManager em, Article art) {
		art.author = em.find(User.class, art.author.id);
		art = em.merge(art);
		return art;
	}

	private Article insert(EntityManager em, Article art) {
		art.id = null;
		art.author = em.find(User.class, art.author.id);
        em.persist(art);
        
        return art;
    }

    private Stream<Article> list(EntityManager em,int offset,int amount,ArtType type) {
   	 TypedQuery<Article> q= em.createQuery("select a from Article a WHERE published = 1 AND" + HSONLY +
   			 					(type == null ? " AND type != 'GIVEAWAYS'" : " AND type = '" + type + "'")+ 
   			 					BYDATE, Article.class);
   	List<Article> art = q.setMaxResults(amount)
   							.setFirstResult(offset)
   							.getResultList();
       return art.stream();
   }
   
    
    
	private Stream<Article> listByCreator(EntityManager em, long id) {
		  List<Article> art = em.createQuery("select a from Article a where published = 1 AND creator = :id AND" + HSONLY + BYDATE, Article.class)
				  .setParameter("id", id)
				  .getResultList();
	        return art.stream();
	}
    
    private Article getByID(EntityManager em,long id) {
    	Article result = em.find(Article.class, id);
    	result.author.loadTwitch();
       return result;
    }
    
    private Article upvote(EntityManager em,long id) {
       Article a = em.find(Article.class, id);
       a.rating = a.rating +1;
       em.merge(a);
       return a;
     }

	@Override
	public CompletionStage<Article> upvote(long id) {
		return supplyAsync(() -> wrap(em -> upvote(em,id)),executionContext);
	}





}
