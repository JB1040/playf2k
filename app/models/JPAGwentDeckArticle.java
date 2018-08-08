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
public class JPAGwentDeckArticle implements GwentDeckArticleRepository  {

	private final JPAApi jpaApi;
	private final DatabaseExecutionContext executionContext;
	private static final String BYDATE = " ORDER BY date DESC";
	private static final String GWONLY = " game = 'GWENT'";

	@Inject
	public JPAGwentDeckArticle(JPAApi jpaApi, DatabaseExecutionContext executionContext) {
		this.jpaApi = jpaApi;
		this.executionContext = executionContext;

	}

	public CompletionStage<DeckGwentArticle> add(DeckGwentArticle art) {
		return supplyAsync(() -> wrap(em -> insert(em, art)), executionContext);
	}

	@Override
	public CompletionStage<DeckGwentArticle> get(long id) {
		return supplyAsync(() -> wrap(em -> getByID(em,id)), executionContext);
	}

	public CompletionStage<Stream<DeckGwentArticle>> list(int offset,int amount,int tier,GwentCard.Faction faction, GwentCard.Leader leader,Game game) {
		return supplyAsync(() -> wrap(em -> list(em,offset,amount,tier,faction,leader,game)), executionContext);
	}


	@Override
	public CompletionStage<Stream<DeckGwentArticle>> byAuthor(long id) {
		return supplyAsync(() -> wrap(em -> listByCreator(em,id)), executionContext);
	}



	public CompletionStage<DeckGwentArticle> edit(DeckGwentArticle art) {
		return supplyAsync(() -> wrap(em -> edit(em,art)),executionContext);
	}

	private <T> T wrap(Function<EntityManager, T> function) {
		return jpaApi.withTransaction(function);
	}



	public DeckGwentArticle edit(EntityManager em, DeckGwentArticle art) {
		art.author = em.find(User.class, art.author.id);
		if (art.editDate == null) {
			DeckGwentArticle art2 = em.find(DeckGwentArticle.class,art.id);
			art.editDate = art2.editDate;
		}
		if (art.cards != null) {
			for (int i = 0; i < art.cards.size(); i++) {
				art.cards.set(i, em.find(GwentCard.class,art.cards.get(i).dbId)); 
			}
		}
		art = em.merge(art);
		return art;
	}

	private DeckGwentArticle insert(EntityManager em, DeckGwentArticle art) {
		art.author = em.find(User.class, art.author.id);
		art.id = null;
		if (art.cards != null) {
			for (int i = 0; i < art.cards.size(); i++) {
				art.cards.set(i, em.find(GwentCard.class,art.cards.get(i).dbId)); 
			}
		}
		em.persist(art);

		return art;
	}

	private Stream<DeckGwentArticle> list(EntityManager em,int offset,int amount,int tier,GwentCard.Faction fac, GwentCard.Leader lead,Game game) {
		TypedQuery<DeckGwentArticle> q= em.createQuery("select a from DeckGwentArticle a WHERE published = 1 AND" + 
				GWONLY + (tier == 0 ? "" : " AND tier = " + tier)+
				(fac == null ? "" : " AND faction = '" + fac + "'") +
				(lead == null ? "" : " AND leader = '" + lead + "'") +
				(game == null ? "" : " AND game = '" + game + "'") +
				BYDATE, DeckGwentArticle.class);
		List<DeckGwentArticle> art = q.getResultList();
		return art.subList( Math.max(0,offset), Math.min(offset+amount,art.size())).stream();
	}



	private Stream<DeckGwentArticle> listByCreator(EntityManager em, long id) {
		List<DeckGwentArticle> art = em.createQuery("select a from DeckGwentArticle a where published = 1 AND creator = :id AND" + GWONLY + BYDATE, DeckGwentArticle.class)
				.setParameter("id", id)
				.getResultList();
		return art.stream();
	}

	private DeckGwentArticle getByID(EntityManager em,long id) {
		if (id == -1) {
			return null;
		}
		DeckGwentArticle result = em.find(DeckGwentArticle.class, id);
    	if (result != null) {
    		result.author.loadTwitch();
    	}
		return result;
	}

	private DeckGwentArticle upvote(EntityManager em,long id) {
		DeckGwentArticle a = em.find(DeckGwentArticle.class, id);
		a.rating = a.rating +1;
		em.persist(a);
		return a;
	}

	@Override
	public CompletionStage<DeckGwentArticle	> upvote(long id) {
		return supplyAsync(() -> wrap(em -> upvote(em,id)),executionContext);
	}





}
