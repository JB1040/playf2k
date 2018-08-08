package controllers;

import akka.actor.ActorSystem;
import javax.inject.*;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.javafx.scene.control.skin.Utils;

import akka.actor.Scheduler;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import akka.util.Timeout;
import models.Article;
import models.Article.Game;
import models.ArticleRepository;
import models.Card;
import models.CardRepository;
import models.DeckArticle;
import models.DeckArticle.Mode;
import models.DeckArticleRepository;
import models.DeckGwentArticle;
import models.GwentCard;
import models.GwentCard.Faction;
import models.GwentCard.Leader;
import models.GwentCardRepository;
import models.GwentDeckArticleRepository;
import models.JPAArticle;
import models.User;
import models.UserRepository;
import models.filters.ArticleFilters;
import models.hibernateModels.BaseArticle;
import models.hibernateModels.BaseCard;
import models.hibernateModels.BaseDeckArticle;
import models.hibernateModels.BaseGwentCard;
import models.hibernateModels.BaseHSLegendDeck;
import models.hibernateModels.BaseHearthstoneCard;
import models.hibernateModels.BaseHearthstoneDeckArticle;
import models.hibernateModels.DeckList;
import models.hibernateModels.HSDeckList;
import play.libs.concurrent.HttpExecutionContext;
import play.core.j.JavaResultExtractor;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.filters.csrf.RequireCSRFCheck;
import play.http.HttpEntity;
import play.libs.Json;
import play.libs.concurrent.HttpExecution;
import play.mvc.*;
import play.mvc.Http.Request;
import play.libs.ws.*;
import java.util.concurrent.Executor;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import controllers.routes;
import enums.General.ArtType;
import enums.General.TextType;
import enums.Gwent.Group;
import enums.Hearthstone.Hero;
import enums.Hearthstone.Set;
import scala.collection.immutable.Stream;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import views.html.deckIndex;
import views.html.deckIndexGW;
import scala.concurrent.ExecutionContextExecutor;

import static play.libs.Json.toJson;
import static play.libs.Json.fromJson;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * The controller for the aeticles API
 */
@Singleton
public class DeckArticlesController extends Controller {

	private final ActorSystem actorSystem;
	private final ExecutionContextExecutor exec;
	private final DeckArticleRepository deckArticleRepository;
	private final GwentDeckArticleRepository gwentDeckArticleRepository;
	private final CardRepository cardRepository;
	private final GwentCardRepository gwentCardRepository;
	private final HttpExecutionContext ec;
	private final UserController userc;
	private final UserRepository userRep;
	private final models.hibernateModels.ArticleRepository artRep2;
	private final models.hibernateModels.CardRepository cardRep2;

	public static  WSClient ws;

	/**
	 * @param actorSystem We need the {@link ActorSystem}'s
	 * {@link Scheduler} to run code after a delay.
	 * @param exec We need a Java {@link Executor} to apply the result
	 * of the {@link CompletableFuture} and a Scala
	 * {@link ExecutionContext} so we can use the Akka {@link Scheduler}.
	 * An {@link ExecutionContextExecutor} implements both interfaces.
	 */
	@Inject
	public DeckArticlesController(ActorSystem actorSystem, ExecutionContextExecutor exec,
			DeckArticleRepository deckArticleRepository,CardRepository cardRepository, 
			HttpExecutionContext ec,WSClient ws,UserController userc,UserRepository userRep,
			GwentDeckArticleRepository gwentDeckArticleRepository,GwentCardRepository gwentCardRepository
			,models.hibernateModels.ArticleRepository rep2,models.hibernateModels.CardRepository cardRep2
			) {
		this.actorSystem = actorSystem;
		this.exec = exec;
		this.deckArticleRepository = deckArticleRepository;
		this.cardRepository = cardRepository;
		this.ec = ec;
		this.userc = userc;
		this.userRep=userRep;
		this.gwentCardRepository = gwentCardRepository;
		this.gwentDeckArticleRepository = gwentDeckArticleRepository;
		this.artRep2 = rep2;
		this.cardRep2 = cardRep2;
		DeckArticlesController.ws = ws;
	}


	//    public CompletionStage<Result> api(String model, String selector) {
	//        return getFutureMessage(0, TimeUnit.SECONDS,model,selector).thenApplyAsync(Results::ok,exec);
	//    }
	//
	//    private CompletionStage<String> getFutureMessage(long time, TimeUnit timeUnit,String model,String selector) {
	//        CompletableFuture<String> future = new CompletableFuture<>();
	//        actorSystem.scheduler().scheduleOnce(
	//            Duration.create(time, timeUnit),
	//            () -> future.complete("Hi2! " + model + " " + selector),
	//            exec
	//        );
	//        return future;
	//    }
	//    

	public CompletionStage<Result> getArticle(long id) {
		return artRep2.get(id).thenApplyAsync(art -> {
			if (art !=null && art.author != null)
				art.author.hashpw = null;
			art.getRecommended().forEach(a -> {
				a.setRecommended(null);
				if (a instanceof BaseDeckArticle) {
					((BaseDeckArticle<?>)a).setDecks(null);
					((BaseDeckArticle<?>)a).setRecommended(null);
					((BaseDeckArticle<?>)a).setSimilar(null);
				}
			});
			//			System.out.println(toJson(  ((BaseDeckArticle)art).getDecks()  )  );
			if (art instanceof BaseDeckArticle) {

				((BaseDeckArticle<?>)art).getSimilar().forEach(a -> {
					a.setDecks(null);
					a.setRecommended(null);
					a.setSimilar(null);
				});
			}
			return ok(toJson(/*(JsonNode)ArticleFilters.noRecommendedNoSimilar.valueToTree(*/art));
		}, ec.current());
	}

	public static java.util.stream.Stream<BaseArticle> loadTwitch(List<BaseArticle> arts) {
		WSRequest req = ArticlesController.ws.url("https://api.twitch.tv/kraken/streams");
		StringBuilder channels = new StringBuilder();
		arts.forEach(art -> {
			if (art.author.twitch != null)
				channels.append(art.author.twitch).append(",");
		});
		if (channels.length() <1)
			return arts.stream();
		channels.setLength(channels.length()-1);
		req.setRequestTimeout(java.time.Duration.ofMillis(8000));
		req.addQueryParameter("channel", channels.toString().toLowerCase())
		.addHeader("Client-ID", "oki24o1s3i52q86ullp92hh1c4wzk9x");
		try {
			return req.get().thenApply(WSResponse::asJson).thenApply(data -> {
				if (data.has("streams")) {
					JsonNode arr = data.get("streams");
					int size = arr.size();
					for (int i = 0; i <size; i++) {
						JsonNode current = arr.get(i);
						arts.forEach(art -> {
							if (current.get("channel").get("name").asText().equals(art.author.twitch)) {
								art.author.twitchData = current;
							}
						});


					}
				}
				return arts;
			}).toCompletableFuture().get().stream();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return arts.stream();
	}

	public static java.util.stream.Stream<DeckGwentArticle> loadTwitch2(List<DeckGwentArticle> arts) {
		WSRequest req = ArticlesController.ws.url("https://api.twitch.tv/kraken/streams");
		StringBuilder channels = new StringBuilder();
		arts.forEach(art -> {
			if (art.author.twitch != null)
				channels.append(art.author.twitch).append(",");
		});
		if (channels.length() <1)
			return arts.stream();
		channels.setLength(channels.length()-1);

		req.setRequestTimeout(java.time.Duration.ofMillis(8000));
		req.addQueryParameter("channel", channels.toString().toLowerCase())
		.addHeader("Client-ID", "oki24o1s3i52q86ullp92hh1c4wzk9x");
		try {
			return req.get().thenApply(WSResponse::asJson).thenApply(data -> {
				if (data.has("streams")) {
					JsonNode arr = data.get("streams");
					int size = arr.size();
					for (int i = 0; i <size; i++) {
						JsonNode current = arr.get(i);
						arts.forEach(art -> {
							if (current.get("channel").get("name").asText().equals(art.author.twitch)) {
								art.author.twitchData = current;
							}
						});


					}
				}
				return arts;
			}).toCompletableFuture().get().stream();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return arts.stream();
	}

	public CompletionStage<Result> getArticles(int offset,int amount,int tier,String mode,String isStandard,String faction,String leader,String game) {

		try {
			enums.General.Game game2 = game == null || game.equals("")? null : enums.General.Game.valueOf(game);
			enums.Hearthstone.Mode mode2 = null;
			Boolean isStandard2 = null;
			if (game2 == null || game2 == enums.General.Game.HS) {

				mode2 = mode == null? null : enums.Hearthstone.Mode.valueOf(mode);
				isStandard2 = isStandard == null? null : Boolean.valueOf(isStandard);
				//int offset,int amount,ArtType type,TextType textType,Game game,int tier,Mode mode,boolean isStandard
			} 
			return artRep2.list(offset,amount, ArtType.DECK, null, game2, tier, mode2, isStandard2).thenApplyAsync(artStream -> {
				//
				return ok((JsonNode)ArticleFilters.noNothing.valueToTree(
						loadTwitch(artStream.collect(Collectors.toList()))
						));
			}, ec.current());
		} catch (IllegalArgumentException e) {
			return supplyAsync(() -> ok(toJson(new ImmutablePair<>("error", "Could not parse parameters."))));
		}
	}

	public CompletionStage<Result> byAuthor(long id) {
		return artRep2.byAuthor(id).thenApplyAsync(artStream -> {
			return ok((JsonNode)ArticleFilters.noRecommended.valueToTree(
					loadTwitch(artStream.collect(Collectors.toList()))
					));
		}, ec.current());
	}

	@Inject FormFactory factory;
	//	@RequireCSRFCheck
	//	public  CompletionStage<Result> doEditInsertArticle() {
	//		Form<DeckArticle> form = factory.form(DeckArticle.class).bindFromRequest();
	//		boolean updateTime = form.field("updateTime").getValue().isPresent();
	//		Request r = play.mvc.Http.Context.current().request();
	//		return userc.isLogged().thenApplyAsync(bool -> {
	//			if (bool) {
	//				if (!form.hasErrors()) {
	//					DeckArticle art =  form.get();
	//					if (updateTime) {
	//						art.editDate = new Date();
	//					}
	//
	//					Long id = art.id;
	//					art = deckArticleRepository.add(art).toCompletableFuture().join();
	//
	//					BaseHearthstoneDeckArticle art2 = new BaseHearthstoneDeckArticle();
	//					art2.authorID = art.author.id;
	//					art2.content=art.content;
	//					art2.editDate = art.editDate;
	//					art2.imageURL = null;
	//					art2.published = art.published;
	//					art2.rating= art.rating;
	//					art2.title= art.title;
	//					art2.game = enums.General.Game.valueOf(art.game.name());
	//					art2.tier = art.tier;
	//					List<HSDeckList> decks2 = new ArrayList<>();
	//
	//					
	//					decks2.add(HSDeckListFromOld(art.cards,art.heroCard.heroClass,art.mode));
	//					art2.setDecks(decks2);
	//					if (id == -1) {
	//						return artRep2.add(art2).thenApplyAsync(art3 -> {
	//
	//							return Results.found("http://d2416peknw0o5h.cloudfront.net/tier_list/_" + art3.id);
	//						}, ec.current()).toCompletableFuture().join();
	//					} else {
	//
	//						art2.id = art.id;
	//						return artRep2.edit(art2).thenApplyAsync(art3 -> {
	//							return Results.found("http://d2416peknw0o5h.cloudfront.net/tier_list/_" + art3.id);
	//						}, ec.current()).toCompletableFuture().join();
	//					}
	//				} else {
	//					return  ok(toJson(new ImmutablePair<>("errors", form.errorsAsJson())));
	//				}
	//			} else {
	//				return redirect("/api/users/login?url=" + request().uri());
	//			}
	//		},ec.current());
	//	}
	//
	//	@play.filters.csrf.AddCSRFToken
	//	public CompletionStage<Result> editInsertArticle(Long id) {
	//		return userc.isLogged().thenApplyAsync(bool -> {
	//			if (bool) {
	//				return (Result) cardRepository.list().thenApplyAsync(art -> {
	//
	//					ArrayNode json = Json.newArray();
	//					art.forEach(obj -> {
	//						json.add(Json.newObject().put("label", (String) obj[0])
	//								.put("actualVal", (Long) obj[1])
	//								.put("cardID", (String) obj[2])
	//								.put("hero", ((Card.Hero) obj[3]).toString()));
	//					});
	//					return json;
	//				},ec.current()).thenCombineAsync(artRep2.get(id), (cards,art3) -> {
	//					DeckArticle art2 = new DeckArticle();
	//					if (art3 != null && art3 instanceof BaseHearthstoneDeckArticle) {
	//						BaseHearthstoneDeckArticle art = (BaseHearthstoneDeckArticle) art3;
	//						art2.author = art.author;
	//						art2.content=art.content;
	//						art2.editDate = art.editDate;
	//						art2.published = art.published;
	//						art2.rating= art.rating;
	//						art2.title= art.title;
	//						art2.game = models.Article.Game.valueOf(art.game.name());
	//						art2.tier = art.tier;
	//						art2.heroClass2 = models.Card.Hero.valueOf(art.heroClass.name());
	//						art2.heroCard.dbId = (long) art.heroClass.dbId;
	//						art2.mode = models.DeckArticle.Mode.valueOf(art.mode.name());
	//					}
	//					List<User> users = (List<User>) userRep.list(0, 200, null).toCompletableFuture().join().collect(Collectors.toList());
	//
	//					return ok(deckIndex.render(art2,toJson(cards),users));
	//				},ec.current()).toCompletableFuture().join();
	//			} else {
	//				return redirect("/api/users/login?url=" + request().uri());
	//			}
	//		},ec.current());
	//	}

	@RequireCSRFCheck
	public  CompletionStage<Result> doEditInsertArticleGW() {
		Form<DeckGwentArticle> form = factory.form(DeckGwentArticle.class).bindFromRequest();
		boolean updateTime = form.field("updateTime").getValue().isPresent();
		Request r = play.mvc.Http.Context.current().request();
		return userc.isLogged().thenApplyAsync(bool -> {
			if (bool) {
				if (!form.hasErrors()) {
					DeckGwentArticle art =  form.get();
					if (updateTime) {
						art.editDate = new Date();
					}
					if (art.id == -1) {
						return gwentDeckArticleRepository.add(art).thenApplyAsync(art2 -> {

							return Results.found("http://d2416peknw0o5h.cloudfront.net/tier_list/_" + art2.id);
						}, ec.current()).toCompletableFuture().join();
					} else {
						return gwentDeckArticleRepository.edit(art).thenApplyAsync(art2 -> {
							return Results.found("http://d2416peknw0o5h.cloudfront.net/tier_list/_" + art2.id);
						}, ec.current()).toCompletableFuture().join();
					}
				} else {
					return  ok(toJson(new ImmutablePair<>("errors", form.errorsAsJson())));
				}
			} else {
				return redirect("/api/users/login?url=" + request().uri());
			}
		},ec.current());
	}

	@play.filters.csrf.AddCSRFToken
	public CompletionStage<Result> editInsertArticleGW(Long id) {
		return userc.isLogged().thenApplyAsync(bool -> {
			if (bool) {
				return (Result) gwentCardRepository.list().thenApplyAsync(art -> {

					ArrayNode json = Json.newArray();
					art.forEach(obj -> {
						json.add(Json.newObject().put("label", (String) obj[0])
								.put("actualVal", (Long) obj[1])
								.put("cardID", (String) obj[2])
								.put("faction", ((GwentCard.Faction)obj[3]).toString()));
					});
					return json;
				},ec.current()).thenCombineAsync(gwentDeckArticleRepository.get(id), (cards,art) -> {
					art = art == null ? new DeckGwentArticle() : art;

					List<User> users = (List<User>) userRep.list(0, 200, null).toCompletableFuture().join().collect(Collectors.toList());

					return ok(deckIndexGW.render(art,toJson(cards),users));
				},ec.current()).toCompletableFuture().join();
			} else {
				return redirect("/api/users/login?url=" + request().uri());
			}
		},ec.current());
	}

	public CompletionStage<Result> upvoteArticle(long id) {
		return deckArticleRepository.upvote(id).thenApplyAsync(art -> {

			return ok(toJson(art));
		},ec.current());

	}

	public CompletionStage<Result> allCards() {
		return cardRepository.list().thenApplyAsync(art -> {
			ArrayNode json = Json.newArray();
			art.forEach(obj -> {

				json.add(Json.newObject().put("label", (String) obj[0]).put("value", (Long) obj[1]));
			});
			return ok(json);
		},ec.current());
	}

	public CompletionStage<Result> allGwentCards() {
		return gwentCardRepository.list().thenApplyAsync(art -> {
			ArrayNode json = Json.newArray();
			art.forEach(obj -> {

				json.add(Json.newObject().put("label", (String) obj[0]).put("value", (Long) obj[1]));
			});
			return ok(json);
		},ec.current());
	}

	//	public CompletionStage<Result> doBeta(Long id) {
	//		//		return cardRepository.all().thenApplyAsync(cards -> {
	//		//			ObjectMapper mapper = new ObjectMapper();
	//		//			ArrayNode arr = mapper.createArrayNode();
	//		//			cards.forEach(card -> {
	//		//				BaseHearthstoneCard obj = fromJson(toJson(card),BaseHearthstoneCard.class);
	//		//				cardRep2.insert(obj);
	//		//				arr.add(toJson(obj));
	//		//			});
	//		//			return ok(arr);
	//		//		});
	//		return deckArticleRepository.get(id).thenApplyAsync(decks -> {
	////			List<BaseArticle> newArts = artRep2.list(0, 10000,enums.General.ArtType.DECK,null, enums.General.Game.HS, 0, null, false).toCompletableFuture().join().collect(Collectors.toList());
	////			if (newArts.size() != 0) {
	////				return ok(toJson(newArts));
	////			}
	////			ObjectMapper mapper = new ObjectMapper();
	////			ArrayNode json = mapper.createArrayNode();
	////
	////
	////			decks.forEach(art -> {
	//			
	//				DeckArticle art = decks;
	//
	//				BaseHearthstoneDeckArticle art2 = new BaseHearthstoneDeckArticle();
	//				
	//				art2.authorID = art.authorID;
	//				art2.content=art.content;
	//				art2.editDate = art.editDate;
	//				art2.imageURL = null;
	//				art2.published = art.published;
	//				art2.rating= art.rating;
	//				art2.title= art.title;
	//				art2.game = enums.General.Game.valueOf(art.game.name());
	//				art2.tier = art.tier;
	//				List<HSDeckList> decks2 = new ArrayList<>();
	//				decks2.add(HSDeckListFromOld(art.cards,art.heroCard.heroClass,art.mode));
	//				art2.setDecks(decks2);
	////			});
	//			return ok(toJson(artRep2.add(art2).toCompletableFuture().join()));
	//		},ec.current());
	//		//		return artRep2.get(id).thenApplyAsync(a -> ok(toJson(a)));
	//	}

	public HSDeckList HSDeckListFromOld(List<Card> oldCards,models.Card.Hero hero,Mode mode) {
		HSDeckList newCards = new HSDeckList();
		List<BaseHearthstoneCard> list = new ArrayList<>();
		for (Card oldC: oldCards) {
			BaseHearthstoneCard c = new BaseHearthstoneCard();
			c.dbId = oldC.dbId;
			list.add(c);
		}

		newCards.setCards(list);
		newCards.heroClass = enums.Hearthstone.Hero.valueOf(hero.name());
		newCards.mode = enums.Hearthstone.Mode.valueOf(mode.name());
		newCards.name = "Main";
		return newCards;
	}

	public CompletionStage<Result> doBetaCards() {
		return artRep2.list(0, 50000, ArtType.DECK, null, null, 0, null, null,true).thenApplyAsync(arts -> {
			AtomicInteger a = new AtomicInteger();
			arts.forEach(art -> {
				BaseDeckArticle<DeckList> bArt = (BaseDeckArticle<DeckList>) art;
				AtomicInteger cost = new AtomicInteger();
				if (bArt.getDecks()!= null) {
					if (art instanceof BaseHearthstoneDeckArticle && bArt.getDecks().size() >=1) {
						((BaseHearthstoneDeckArticle) art).isStandard = ((HSDeckList) bArt.getDecks().get(0)).isStandard;
					}
					if (bArt.getDecks().size() >=1 && bArt.getDecks().get(0).getCards() != null)
						bArt.getDecks().get(0).getCards().forEach(card -> {
							if (card instanceof BaseHearthstoneCard && ((BaseHearthstoneCard)card).set != Set.CORE) {
								cost.addAndGet(((BaseHearthstoneCard)card).rarity.cost);
							} else 	if (card instanceof BaseGwentCard && ((BaseGwentCard)card).group != Group.LEADER) {
								cost.addAndGet(((BaseGwentCard)card).rarity.cost);
							}
						});

					bArt.cost = cost.intValue();
					bArt.getDecks().forEach(deck -> {
						if  (deck instanceof HSDeckList) {
							((HSDeckList) deck).code = utils.HearthstoneUtils.getCodeFromList(
									((HSDeckList) deck).heroClass, 
									((HSDeckList) deck).isStandard,
									((HSDeckList) deck).getCards());
						}
					});
					artRep2.edit(art, null,null).toCompletableFuture().join();
				}
				return;
			});
			return ok();

		});

		//		return cardRep2.HSlist().thenApplyAsync(newArts -> {
		//			List<BaseHearthstoneCard> newArts2 = newArts.collect(Collectors.toList());
		//			System.out.println(toJson(newArts2));
		//			if (newArts2.size() != 0) {
		//				System.out.println("EXISTS");
		//				return ok(toJson(newArts2));
		//			} else {
		//				return cardRepository.all().thenApplyAsync(cards -> {
		//					ObjectMapper mapper = new ObjectMapper();
		//					ArrayNode arr = mapper.createArrayNode();
		//					cards.forEach(card -> {
		//						BaseHearthstoneCard obj = fromJson(toJson(card),BaseHearthstoneCard.class);
		//						cardRep2.insert(obj);
		//						arr.add(toJson(obj));
		//					});
		//					return ok(arr);
		//				}).toCompletableFuture().join();
		//			}
		//		});
	}

	public CompletionStage<Result> doBetaCardsGW() {
		return artRep2.list(0, 50000, ArtType.TIERDECK, null, null, 0, null, null,true).thenApplyAsync(arts -> {
			AtomicInteger a = new AtomicInteger();
			arts.forEach(art -> {
				BaseDeckArticle<DeckList> bArt = (BaseDeckArticle<DeckList>) art;
				AtomicInteger cost = new AtomicInteger();

				if (bArt.getDecks()!= null) {
					if (art instanceof BaseHearthstoneDeckArticle && bArt.getDecks().size() >=1) {
						((BaseHearthstoneDeckArticle) art).isStandard = ((HSDeckList) bArt.getDecks().get(0)).isStandard;
					}
					if (bArt.getDecks().size() >=1 && bArt.getDecks().get(0).getCards() != null)
						bArt.getDecks().get(0).getCards().forEach(card -> {
							if (card instanceof BaseHearthstoneCard && ((BaseHearthstoneCard)card).set != Set.CORE) {
								cost.addAndGet(((BaseHearthstoneCard)card).rarity.cost);
							} else 	if (card instanceof BaseGwentCard && ((BaseGwentCard)card).group != Group.LEADER) {
								cost.addAndGet(((BaseGwentCard)card).rarity.cost);
							}
						});

					bArt.cost = cost.intValue();
					bArt.getDecks().forEach(deck -> {
						if  (deck instanceof HSDeckList) {
							((HSDeckList) deck).code = utils.HearthstoneUtils.getCodeFromList(
									((HSDeckList) deck).heroClass, 
									((HSDeckList) deck).isStandard,
									((HSDeckList) deck).getCards());
						}
					});
					artRep2.edit(art, null,null).toCompletableFuture().join();
				}
				return;
			});
			return ok();

		});
	}



}
