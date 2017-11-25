package models;


import play.db.jpa.JPAApi;
import play.mvc.BodyParser.Json;
import play.db.*;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;

import models.Article.Game;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Provide JPA operations running inside of a thread pool sized to the connection pool
 */
public class JPADeckArticle implements DeckArticleRepository  {

	private final JPAApi jpaApi;
	private final DatabaseExecutionContext executionContext;
	private static final String BYDATE = " ORDER BY date DESC";
	private static final String HSONLY = " game = 'HS'";

	@Inject
	public JPADeckArticle(JPAApi jpaApi, DatabaseExecutionContext executionContext) {
		this.jpaApi = jpaApi;
		this.executionContext = executionContext;

	}

	public CompletionStage<DeckArticle> add(DeckArticle art) {
		return supplyAsync(() -> wrap(em -> insert(em, art)), executionContext);
	}

	@Override
	public CompletionStage<DeckArticle> get(long id) {
		return supplyAsync(() -> wrap(em -> getByID(em,id)), executionContext);
	}

	public CompletionStage<Stream<DeckArticle>> list(int offset,int amount,int tier,DeckArticle.Mode mode, Boolean isStandard,Game game) {
		return supplyAsync(() -> wrap(em -> list(em,offset,amount,tier,mode,isStandard,game)), executionContext);
	}


	@Override
	public CompletionStage<Stream<DeckArticle>> byAuthor(long id) {
		return supplyAsync(() -> wrap(em -> listByCreator(em,id)), executionContext);
	}



	@Override
	public CompletionStage<DeckArticle> edit(DeckArticle art) {
		return supplyAsync(() -> wrap(em -> edit(em,art)),executionContext);
	}

	private <T> T wrap(Function<EntityManager, T> function) {
		return jpaApi.withTransaction(function);
	}



	public DeckArticle edit(EntityManager em, DeckArticle art) {
		art.author = em.find(User.class, art.author.id);
		if (art.editDate == null) {
			DeckArticle art2 = em.find(DeckArticle.class,art.id);
			art.editDate = art2.editDate;
		}
		art.heroCard = art.heroCard.dbId == -1 ?  null: em.find(Card.class, art.heroCard.dbId);
		if (art.cards != null) {
			for (int i = 0; i < art.cards.size(); i++) {
				art.cards.set(i, em.find(Card.class,art.cards.get(i).dbId)); 
			}
		}
		art = em.merge(art);
		return art;
	}

	private DeckArticle insert(EntityManager em, DeckArticle art) {
		art.author = em.find(User.class, art.author.id);
		art.id = null;
		art.heroCard = art.heroCard.dbId == -1 ?  null: em.find(Card.class, art.heroCard.dbId);
		if (art.cards != null) {
			for (int i = 0; i < art.cards.size(); i++) {
				art.cards.set(i, em.find(Card.class,art.cards.get(i).dbId)); 
			}
		}
		em.persist(art);

		return art;
	}

	private Stream<DeckArticle> list(EntityManager em,int offset,int amount,int tier,DeckArticle.Mode mode,Boolean isStandard,Game game) {
		TypedQuery<DeckArticle> q= em.createQuery("select a from DeckArticle a WHERE published = 1 AND" + 
				HSONLY + (tier == 0 ? "" : " AND tier = " + tier)+
				(mode == null ? "" : " AND mode = '" + mode + "'") +
				(game == null ? "" : " AND game = '" + game + "'") +
				BYDATE, DeckArticle.class);
		List<DeckArticle> art = q.getResultList();
		art.removeIf(d -> isStandard !=null && d.isStandard != isStandard);
		return art.subList( Math.max(0,offset), Math.min(offset+amount,art.size())).stream();
	}



	private Stream<DeckArticle> listByCreator(EntityManager em, long id) {
		List<DeckArticle> art = em.createQuery("select a from DeckArticle a where published = 1 AND authorID = :id AND" + HSONLY + BYDATE, DeckArticle.class)
				.setParameter("id", id)
				.getResultList();
		return art.stream();
	}

	private DeckArticle getByID(EntityManager em,long id) {
		if (id == -1) {
			return null;
		}
		DeckArticle result = em.find(DeckArticle.class, id);
    	if (result != null) {
    		result.author.loadTwitch();
    	}
		return result;
	}

	private DeckArticle upvote(EntityManager em,long id) {
		DeckArticle a = em.find(DeckArticle.class, id);
		a.rating = a.rating +1;
		em.persist(a);
		return a;
	}

	@Override
	public CompletionStage<DeckArticle> upvote(long id) {
		return supplyAsync(() -> wrap(em -> upvote(em,id)),executionContext);
	}





}
