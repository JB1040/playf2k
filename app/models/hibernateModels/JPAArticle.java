package models.hibernateModels;

import play.db.jpa.JPAApi;
import play.db.*;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;

import org.hibernate.Session;
import org.hibernate.transform.ResultTransformer;

import enums.General.ArtType;
import enums.General.Difficulty;
import enums.General.Game;
import enums.General.TextType;
import enums.Hearthstone.Hero;
import enums.Hearthstone.Mode;
import enums.Hearthstone.Set;
import models.Card;
import models.DatabaseExecutionContext;
import models.DeckArticle;
import models.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import static play.libs.Json.toJson;
/**
 * Provide JPA operations running inside of a thread pool sized to the
 * connection pool
 */
public class JPAArticle implements ArticleRepository {

	private final JPAApi jpaApi;
	private final DatabaseExecutionContext executionContext;
	private static final String BYDATE = " ORDER BY editDate DESC";

	@Inject
	public JPAArticle(JPAApi jpaApi, DatabaseExecutionContext executionContext) {
		this.jpaApi = jpaApi;
		this.executionContext = executionContext;
	}

	public CompletionStage<BaseArticle> add(BaseArticle art,List<Long> recommended,List<Long> similar) {

		return supplyAsync(() -> wrap(em -> insert(em, art,recommended,similar)), executionContext);
	}

	@Override
	public CompletionStage<BaseArticle> get(long id) {
		return supplyAsync(() -> wrap(em -> getByID(em, id)), executionContext);
	}

	public CompletionStage<Stream<BaseArticle>> list(int offset, int amount, ArtType type,TextType textType, Game game,int tier, Mode mode, Boolean isStandard) {
		return list(offset, amount, type, textType, game, tier, mode, isStandard, false);
	}

	public CompletionStage<Stream<BaseArticle>> list(int offset, int amount, ArtType type,TextType textType, Game game,int tier, Mode mode, Boolean isStandard,boolean forceAll) {
		return supplyAsync(() -> wrap(em -> list(em, offset, amount, type,textType, game,tier,mode,isStandard,forceAll)), executionContext);
	}

	public CompletionStage<Stream<Object>> tierlist(int offset, int amount, Game game,int minRank,int maxRank, Mode mode, Boolean isStandard,Hero[] heroes) {
		return supplyAsync(() -> wrap(em -> tierlist(em, offset, amount, game,minRank, maxRank,mode,isStandard,heroes)), executionContext);
	}

	@Override
	public CompletionStage<Stream<BaseArticle>> byAuthor(long id) {
		return supplyAsync(() -> wrap(em -> listByCreator(em, id)), executionContext);
	}

	@Override
	public CompletionStage<BaseArticle> edit(BaseArticle art,List<Long> recommended,List<Long> similar) {
		return supplyAsync(() -> wrap(em -> edit(em, art,recommended,similar)), executionContext);
	}

	private <T> T wrap(Function<EntityManager, T> function) {
		return jpaApi.withTransaction(function);
	}

	public BaseArticle edit(EntityManager em, BaseArticle art,List<Long> recommended,List<Long> similar) {

		setRecommended(em,art,recommended);

		art.author = em.find(User.class, (art.author != null ? art.author.id : art.authorID));

		BaseArticle art2 = em.find(BaseArticle.class,art.id);
		art.rating = art2.rating;
		if (art.editDate == null || art.updateEdit == false) {
			art.editDate = art2.editDate;
		}
		art.date = art2.date;
		if (art.date == null) {
			art.date = art.editDate;
		}

		if (art instanceof BaseDeckArticle) {
			List<HSDeckList> decks = ((BaseHearthstoneDeckArticle) art2).getDecks();
			for (int i = 0;  i < decks.size();i++) {
				em.remove(decks.get(i));
			}
			decks.clear();
			art2 = (BaseDeckArticle<?>) em.merge(art);
			art = editDeck(em,(BaseDeckArticle<?>)art,decks);
			setSimilar(em,(BaseDeckArticle<?>)art,similar);
		} else {
			art = em.merge(art);
		}
		return art;
	}

	private BaseArticle insert(EntityManager em, BaseArticle art,List<Long> recommended,List<Long> similar) {
		art.id=null;
		setRecommended(em,art,recommended);
		art.author = em.find(User.class, (art.author != null ? art.author.id : art.authorID));
		if (art.editDate == null) {
			art.editDate = new java.util.Date();
			if (art.date == null) {
				art.date = new java.util.Date();
			}
		} else if (art.date == null) {
			art.date=art.editDate;
		}
		if (art instanceof BaseHearthstoneDeckArticle) {
			((BaseHearthstoneDeckArticle) art).heroClass =Hero.NEUTRAL;
			((BaseHearthstoneDeckArticle) art).mode =Mode.CON;
		}
		em.persist(art);
		if (art instanceof BaseDeckArticle) {
			insertDeck(em,(BaseDeckArticle<?>)art);

			setSimilar(em,(BaseDeckArticle<?>)art,similar);
		}
		return art;
	}


	private void insertDeck(EntityManager em, BaseDeckArticle<?> art) {
		art.getDecks().forEach(d ->  {
			List<BaseCard> cards = d.getCards();
			if (cards != null) {
				boolean stnd = true;
				for (int i = 0; i < cards.size(); i++) {
					cards.set(i, em.find(BaseCard.class,cards.get(i).dbId)); 
					if (d instanceof HSDeckList) {
						BaseHearthstoneCard c = (BaseHearthstoneCard)cards.get(i);
						if (c.set != null && !HSDeckList.stdSets.contains(c.set)) {
							stnd = false;
						}
						((HSDeckList) d).isStandard = stnd;
					}
				}
				
				
			}
			d.baseDeckArticle = art;
			if (d instanceof HSDeckList)
				((HSDeckList) d).code = utils.HearthstoneUtils.getCodeFromList(((HSDeckList) d).heroClass, 
						((HSDeckList) d).isStandard, cards.stream().map(a -> (BaseHearthstoneCard) a).collect(Collectors.toList()));
			em.persist(d);
		});
		if ( art instanceof BaseHearthstoneDeckArticle) {
			BaseHearthstoneDeckArticle arth = (BaseHearthstoneDeckArticle) art;
			if (arth.decks != null && arth.decks.size() > 0 ) {
				HSDeckList d = (HSDeckList) art.decks.get(0);
				boolean stand = true;
				int cost = 0;
				List<BaseHearthstoneCard> cards = d.getCards();
				if (cards != null) {
					for (BaseHearthstoneCard c: cards) {
						if ( c.set != Set.CORE)
							cost += c.rarity.cost;
						if (c.set != null && !HSDeckList.stdSets.contains(c.set)) {
							stand = false;
						}
					}
				}
				arth.heroClass = d.heroClass;
				arth.cost = cost;
				arth.isStandard = stand;
				arth.mode = d.mode;
				d.code = utils.HearthstoneUtils.getCodeFromList(d.heroClass, stand, cards);
				em.merge(d);
				em.merge(arth);
			}
		}
	}

	private BaseArticle editDeck(EntityManager em, BaseDeckArticle<?> art,List<? extends DeckList> decks) {
		Integer curr = 0;
		for (int i = art.getDecks().size();  i < decks.size();i++) {
			em.remove(decks.get(i));
			decks.remove(i);
		}

		if (art instanceof BaseHearthstoneDeckArticle) {
			BaseHearthstoneDeckArticle artH = (BaseHearthstoneDeckArticle)art;
			for (int j = 0; j < artH.getDecks().size(); j++) {
				HSDeckList d = artH.getDecks().get(j);


				List<BaseHearthstoneCard> cards = d.getCards();
				if (cards != null) {
					for (int i = 0; i < cards.size(); i++) {
						cards.set(i, em.find(BaseHearthstoneCard.class,cards.get(i).dbId)); 
					}
				}
				boolean stand = true;
				if (cards != null && stand) {
					for (BaseHearthstoneCard c: cards) {
						if (c.set != null && !HSDeckList.stdSets.contains(c.set)) {
							stand = false;
							break;
						}
					}
				}
				d.code = utils.HearthstoneUtils.getCodeFromList(d.heroClass, stand, cards);
				if (decks.size()> j) {
					HSDeckList d2 = (HSDeckList) decks.get(curr);
					d2.name = d.name;
					d2.mode = d.mode;
					d2.heroClass = d.heroClass;
					d2.setCards(cards);
					d2.code = d.code;
					artH.getDecks().set(j, d2); 
				} 

			}

			if (artH.decks != null && artH.decks.size() > 0 ) {
				HSDeckList d = (HSDeckList) artH.decks.get(0);
				boolean stand = true;
				List<BaseHearthstoneCard> cards = d.getCards();
				int cost = 0;
				if (cards != null) {
					for (BaseHearthstoneCard c: cards) {
						if (c.set != Set.CORE) {
							cost += c.rarity.cost;
						}
						if (c.set != null && !HSDeckList.stdSets.contains(c.set)) {
							stand = false;
						}
					}
				}
				artH.heroClass = d.heroClass;
				artH.cost = cost;
				artH.isStandard = stand;
				artH.mode = d.mode;
			}
		}
		BaseHearthstoneDeckArticle art2 = (BaseHearthstoneDeckArticle)em.merge(art);

		art.getDecks().forEach(d ->  {

			d.baseDeckArticle = art2;
			if (d.id == null) {
				em.persist(d);
			}	else {

				em.merge(d);
			}
		});

		return em.merge(art);

	}

	private void setRecommended(EntityManager em, BaseArticle art,List<Long> rec) {
		Session session = em.unwrap(Session.class);
		if (rec != null) {
			List<BaseArticle> recs = session.byMultipleIds(BaseArticle.class).enableSessionCheck(true).multiLoad(rec);
			art.setRecommended(new HashSet<BaseArticle>(recs));
		}
	}


	private void setSimilar(EntityManager em, BaseDeckArticle<?> art, List<Long> similar) {
		Session session = em.unwrap(Session.class);
		if (similar != null) {
			if (art instanceof BaseHearthstoneDeckArticle) {
				List<? extends BaseDeckArticle<HSDeckList>>  recs = session.byMultipleIds(BaseHearthstoneDeckArticle.class).enableSessionCheck(true).multiLoad(similar);
				((BaseHearthstoneDeckArticle)art).setSimilar(new HashSet<>(recs));
			} else if (art instanceof BaseGwentDeckArticle) {
				List<? extends BaseDeckArticle<GwentDeckList>>  recs = session.byMultipleIds(BaseGwentDeckArticle.class).enableSessionCheck(true).multiLoad(similar);
				((BaseGwentDeckArticle)art).setSimilar(new HashSet<>(recs));
			}
		}
	}
	private Stream<Object> tierlist(EntityManager em,int offset, int amount, Game game,int minRank,int maxRank, Mode mode, Boolean isStandard,Hero[] heroes) {
		Class<? extends BaseArticle> cl =(game == null? BaseDeckArticle.class: (game == Game.HS ? BaseHSLegendDeck.class : BaseGwentDeckArticle.class));
		StringBuilder query = new StringBuilder("select a from " );
		query.append(game == null? "BaseDeckArticle": (game == Game.HS ? "BaseHSLegendDeck" : "BaseGwentDeckArticle"))
		.append(" a WHERE published = 1")
		.append(BYDATE);


		List<BaseArticle> art = null; 
		TypedQuery<? extends BaseArticle> q = em.createQuery(query.toString(), cl);
		art = (List<BaseArticle>) q.getResultList();
		int length = -1;
		if (game == Game.HS) {
			art.removeIf(d -> 
			((isStandard != null && ((BaseHearthstoneDeckArticle)d).isStandard != isStandard) ||
					(mode       != null && ((BaseHearthstoneDeckArticle)d).mode != mode))
					);
			if (heroes != null && heroes.length > 0) {
				art.removeIf(d -> {
					for (Hero h : heroes) {
						if (((BaseHearthstoneDeckArticle) d).heroClass.equals(h)) return false;
					}
					return true;
				});

			}

		}

		length = art.size();
		art = art.subList(Math.max(0,Math.min(length-1,offset)), Math.max(0,Math.min(length, offset+amount)));
		List<Object> result = new ArrayList<>();
		result.add(art);
		if (offset == 0) {
			result.add(length);
		}
		return result.stream();

	}

	//offset,amount, ArtType.DECK, null, game2, tier, mode2, isStandard2
	private Stream<BaseArticle> list(EntityManager em, int offset, int amount, ArtType type,TextType textType, Game game,int tier, Mode mode, Boolean isStandard,boolean forceAll) {
		Class<? extends BaseArticle> cl = type == ArtType.ARTICLE ? TextArticle.class : 
			(game == null? BaseDeckArticle.class: (game == Game.HS ? BaseHearthstoneDeckArticle.class : BaseGwentDeckArticle.class));

		StringBuilder query = new StringBuilder("select a from " );
		query.append(type == ArtType.ARTICLE ? "TextArticle" : ( game == null? "BaseDeckArticle": (game == Game.HS ? "BaseHearthstoneDeckArticle" : "BaseGwentDeckArticle")))
		.append(" a WHERE ")
		.append(forceAll ? "(published = 0 OR published = 1)" : "published = 1");

		List<BaseArticle> art = null; 


		if (type == ArtType.ARTICLE) {
			query.append(textType == null ? " AND type != 'GIVEAWAYS'" : " AND type = '" + textType + "'")
			.append(game == null ? "" : " AND game = '" + game + "'");
			query.append(BYDATE);
			TypedQuery<? extends BaseArticle> q = em.createQuery(query.toString(), cl);
			art = (List<BaseArticle>) q.setMaxResults(amount).setFirstResult(offset).getResultList();
		} else if (type == ArtType.DECK) {
			query.append(tier == 0 ? "" : " AND tier = " + tier);
			
			query.append(!forceAll ? " AND player IS NULL" : "");
			Long time = System.currentTimeMillis();
			query.append(isStandard != null? " AND isStandard = " + isStandard : "");
			query.append(mode != null? " AND mode = '" + mode + "'": "");
			query.append(" ORDER BY tier ASC, importance DESC");
			TypedQuery<? extends BaseArticle> q = em.createQuery(query.toString(), cl);
			art = (List<BaseArticle>) q.setMaxResults(amount).setFirstResult(offset).getResultList();
			if (forceAll) {
				art.forEach(a -> ((BaseDeckArticle)a).getDecks());
			}
			//			art = (List<BaseArticle>)test;

		}

		//		query.append(BYDATE);

		return art.stream();
	}

	private Stream<BaseArticle> listByCreator(EntityManager em, long id) {
		List<BaseArticle> art = em
				.createQuery("select a from BaseArticle a where published = 1 AND authorID = :id " + BYDATE,
						BaseArticle.class)
				.setParameter("id", id).getResultList();
		return art.stream();
	}

	private BaseArticle getByID(EntityManager em, long id) {
		if (id == -1) {
			return null;
		}
		if (id > 150 && id <500) {
			id +=500;
		}
		BaseArticle result = em.find(BaseArticle.class, id);
		if (result != null) {
			result.author.loadTwitch();
		} 
		if (result instanceof BaseDeckArticle) {
			List<DeckList> d = ((BaseDeckArticle) result).getDecks();
		}

		if (result.recommended == null || result.recommended.size() == 0) {
			getAndSetRecommended(em,result);
		}
		if (result instanceof BaseDeckArticle) {
			BaseDeckArticle r = (BaseDeckArticle) result;
			if (r.similar == null || r.similar.size() == 0) {
				getAndSetSimilar(em,r);
			}
		}

		return result;
	}

	private void getAndSetRecommended(EntityManager em,BaseArticle result) {
		if (result instanceof TextArticle) {
			List<? extends BaseArticle> arts = em.createQuery("select a from TextArticle a where published = 1 AND id <> :id " + BYDATE,
					TextArticle.class)
					.setParameter("id", result.id).setMaxResults(6).getResultList();
			result.setRecommended(new LinkedHashSet<BaseArticle>(arts));
		} else if (result.recommended == null){
			result.recommended = new LinkedHashSet<BaseArticle>();
		}
	}

	private void getAndSetSimilar(EntityManager em,BaseDeckArticle result) {
		if (result instanceof BaseHSLegendDeck) {
			List<BaseHSLegendDeck> arts = em.createQuery("select a from BaseHSLegendDeck a where published = 1 AND id <> :id " + BYDATE,
					BaseHSLegendDeck.class)
					.setParameter("id", result.id).setMaxResults(6).getResultList();
			((BaseHearthstoneDeckArticle)result).setSimilar(new LinkedHashSet<BaseDeckArticle<HSDeckList>>(arts));
		} else if (result instanceof BaseHearthstoneDeckArticle) {
			List<? extends BaseDeckArticle<HSDeckList>> arts = em.createQuery("select a from BaseHearthstoneDeckArticle a where published = 1 AND (tier = 1 OR tier = 2) AND id <> :id " + BYDATE,
					BaseHearthstoneDeckArticle.class)
					.setParameter("id", result.id).setMaxResults(6).getResultList();
			((BaseHearthstoneDeckArticle)result).setSimilar(new LinkedHashSet<BaseDeckArticle<HSDeckList>>(arts));
		} else if (result instanceof BaseGwentDeckArticle) {
			List<? extends BaseDeckArticle<GwentDeckList>> arts = em.createQuery("select a from BaseGwentDeckArticle a where published = 1 AND id <> :id " + BYDATE,
					BaseGwentDeckArticle.class)
					.setParameter("id", result.id).setMaxResults(6).getResultList();
			((BaseGwentDeckArticle)result).setSimilar(new LinkedHashSet<BaseDeckArticle<GwentDeckList>>(arts));
		} 
	}

	private BaseArticle upvote(EntityManager em, long id) {
		if (id > 150 && id <500) {
			id +=500;
		}
		BaseArticle a = em.find(BaseArticle.class, id);
		a.rating = a.rating + 1;
		em.merge(a);
		return a;
	}

	@Override
	public CompletionStage<BaseArticle> upvote(long id) {
		return supplyAsync(() -> wrap(em -> upvote(em, id)), executionContext);
	}



}
