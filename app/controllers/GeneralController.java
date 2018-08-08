package controllers;

import akka.actor.ActorSystem;
import javax.inject.*;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import akka.actor.Scheduler;
import models.AdRepository;
import models.Advertisement;
import models.Article;
import enums.General.ArtType;
import enums.General.Game;
import enums.Hearthstone.Hero;
import enums.Hearthstone.Mode;
import models.ArticleFeatured.Target;
import models.ArticleRepository;
import models.FeaturedRepository;
import models.JPAArticle;
import models.User;
import models.UserRepository;
import models.filters.ArticleFilters;
import models.hibernateModels.BaseArticle;
import models.hibernateModels.BaseCard;
import models.hibernateModels.BaseDeckArticle;
import models.hibernateModels.BaseGwentCard;
import models.hibernateModels.BaseGwentDeckArticle;
import models.hibernateModels.BaseHSLegendDeck;
import models.hibernateModels.BaseHearthstoneCard;
import models.hibernateModels.BaseHearthstoneDeckArticle;
import models.hibernateModels.CardRepository;
import models.hibernateModels.DeckList;
import models.hibernateModels.GwentDeckList;
import models.hibernateModels.HSDeckList;
import models.hibernateModels.TextArticle;
import play.libs.concurrent.HttpExecutionContext;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.filters.csrf.RequireCSRFCheck;
import play.libs.concurrent.HttpExecution;
import play.mvc.*;
import play.mvc.Http.Request;
import play.libs.ws.*;
import java.util.concurrent.Executor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import scala.collection.immutable.Stream;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import views.html.articleIndex;
import views.html.generalindex;
import views.html.advertisementIndex;
import views.html.overlaySQR;
import views.html.overlayREC;
import scala.concurrent.ExecutionContextExecutor;
import static play.libs.Json.toJson;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * The controller for the aeticles API
 */
@Singleton
public class GeneralController extends Controller {

	private final ActorSystem actorSystem;
	private final ExecutionContextExecutor exec;
	private final models.hibernateModels.ArticleRepository artRep;
	private final AdRepository adRepository;
	private final HttpExecutionContext ec;
	private final UserController userc;
	private final UserRepository userRep;
	public static  WSClient ws;

	private final Pattern youtube = Pattern.compile("(https?://)?(www\\.)?(youtube\\.com/watch\\?.*?v=|youtu\\.be/)(?<code>[^&]*)", Pattern.CASE_INSENSITIVE);
	private  final FeaturedRepository featuredRepository;
	private final CardRepository cardRep;

	/**
	 * @param actorSystem We need the {@link ActorSystem}'s
	 * {@link Scheduler} to run code after a delay.
	 * @param exec We need a Java {@link Executor} to apply the result
	 * of the {@link CompletableFuture} and a Scala
	 * {@link ExecutionContext} so we can use the Akka {@link Scheduler}.
	 * An {@link ExecutionContextExecutor} implements both interfaces.
	 */
	@Inject
	public GeneralController(ActorSystem actorSystem, ExecutionContextExecutor exec,
			AdRepository adRepository,HttpExecutionContext ec,
			UserController u,WSClient ws,UserRepository userRep,FeaturedRepository featuredRepository
			,models.hibernateModels.ArticleRepository rep
			,models.hibernateModels.CardRepository rep2
			) {
		this.actorSystem = actorSystem;
		this.exec = exec;
		this.ec = ec;
		GeneralController.ws = ws;
		this.featuredRepository = featuredRepository;
		this.userRep = userRep;
		this.userc = u;
		this.adRepository = adRepository;
		this.artRep =rep;
		this.cardRep =rep2;
	}



	public CompletionStage<Result> getArticle(long id) {
		return artRep.get(id).thenApplyAsync(art -> {
			art.author.hashpw = null;
			((BaseArticle)art).getRecommended().forEach(a -> {
				a.setRecommended(null);
				if (a instanceof BaseDeckArticle) {
					((BaseDeckArticle<?>)a).setSimilar(null);
					((BaseDeckArticle<?>)a).setDecks(null);
					((BaseDeckArticle<?>)a).setSimilar(null);
				}
			});
			if (art instanceof BaseDeckArticle) {
				((BaseDeckArticle<?>)art).getSimilar().forEach(a -> {
					a.setDecks(null);
					a.setRecommended(null);
					a.setSimilar(null);
				});
			}
			return ok(toJson(art));
		}, ec.current());
	}

	public CompletionStage<Result> getTierArticles(int offset,int amount,String game,String isStandard,String heroes) {
		try {
			enums.General.Game game2 = game == null || game.equals("")? null : enums.General.Game.valueOf(game);
			Boolean isStd = isStandard == null || isStandard.equals("")? null : Boolean.valueOf(isStandard);
			String[] heroTemp = heroes == null? new String[] {} : heroes.split(",");
			Hero[] heroes2 = new Hero[heroTemp.length];
			for (int i = 0; i < heroTemp.length;i++) {
				heroes2[i] = Hero.valueOf(heroTemp[i]);
			}
			return artRep.tierlist(offset,amount,game2, -1,-1,Mode.CON, isStd,heroes2).thenApplyAsync(artStream -> {
				return ok((JsonNode)ArticleFilters.noNothing.valueToTree(artStream.collect(Collectors.toList())));
			}, ec.current());
		} catch (IllegalArgumentException e) {
			return supplyAsync(() -> ok(toJson(new ImmutablePair<>("error", "Could not parse parameters."))));
		}
	}



	public CompletionStage<Result> getArticles(int offset,int amount,String type,String game) {
		try {
			enums.General.TextType type2 = type == null || type.equals("")? null : enums.General.TextType.valueOf(type);
			enums.General.Game game2 = game == null || game.equals("")? null : enums.General.Game.valueOf(game);
			return artRep.list(offset,amount,enums.General.ArtType.ARTICLE, type2,game2, 0, null, false).thenApplyAsync(artStream -> {
				return ok((JsonNode)ArticleFilters.noNothing.valueToTree(artStream));
			}, ec.current());
		} catch (IllegalArgumentException e) {
			return supplyAsync(() -> ok(toJson(new ImmutablePair<>("error", "Could not parse parameters."))));
		}
	}

	public CompletionStage<Result> byAuthor(long id) {
		return artRep.byAuthor(id).thenApplyAsync(artStream -> {
			return ok(toJson(loadTwitch(artStream.collect(Collectors.toList()))));
		}, ec.current());
	}

	public CompletionStage<Result> getFeatured() {
		return featuredRepository.list().thenApplyAsync(artStream -> {
			//artStream = artStream.map((art) -> {	art.author.hashpw = null; return art;});
			return ok(toJson(artStream.map(art -> {
				if (art.target == Target.ARTICLE) {
					art.article.author.loadTwitch();
					return art.article;
				} else {
					art.deck.author.loadTwitch();
					return art.deck;
				}
			})));
		}, ec.current());
	}

	public static java.util.stream.Stream<BaseArticle> loadTwitch(List<BaseArticle> arts) {
		WSRequest req = GeneralController.ws.url("https://api.twitch.tv/kraken/streams");
		StringBuilder channels = new StringBuilder();

		arts.forEach(art -> {
			if (art.author.twitch != null)
				channels.append(art.author.twitch).append(",");
		}) ;

		if (channels.length() <1)
			return arts.stream();
		channels.setLength(channels.length()-1);

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
			e.printStackTrace();
		}
		return arts.stream();
	}

	public static java.util.stream.Stream<Object> loadTwitch2(List<Object> arts) {
		WSRequest req = GeneralController.ws.url("https://api.twitch.tv/kraken/streams");
		StringBuilder channels = new StringBuilder();
		List<BaseHearthstoneDeckArticle> art = (List<BaseHearthstoneDeckArticle>) arts.get(0);
		art.forEach(art2 -> {
			if (art2.author.twitch != null)
				channels.append(art2.author.twitch).append(",");
		}) ;

		if (channels.length() <1)
			return arts.stream();
		channels.setLength(channels.length()-1);

		req.addQueryParameter("channel", channels.toString().toLowerCase())
		.addHeader("Client-ID", "oki24o1s3i52q86ullp92hh1c4wzk9x");
		try {
			return req.get().thenApply(WSResponse::asJson).thenApply(data -> {
				if (data.has("streams")) {
					JsonNode arr = data.get("streams");
					int size = arr.size();
					for (int i = 0; i <size; i++) {
						JsonNode current = arr.get(i);
						art.forEach(art2 -> {
							if (current.get("channel").get("name").asText().equals(art2.author.twitch)) {
								art2.author.twitchData = current;
							}
						});


					}
				}
				return arts;
			}).toCompletableFuture().get().stream();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return arts.stream();
	}


	@Inject FormFactory factory;
	@RequireCSRFCheck
	public  CompletionStage<Result> doEditInsertArticle() {



		Request r = play.mvc.Http.Context.current().request();
		return userc.isLogged().thenApplyAsync(bool -> {
			if (bool) {
				DynamicForm form = factory.form().bindFromRequest("artType","game");
				String artTypeStr = form.field("artType").getValue().orElse(null);
				ArtType artType = artTypeStr == null ? null : ArtType.valueOf(artTypeStr);
				Form<? extends BaseArticle> artForm = null;
				BaseArticle art;
				if (artType == ArtType.ARTICLE) {
					artForm = factory.form(TextArticle.class).bindFromRequest();
				} else if (artType == ArtType.DECK) {
					String gameStr = form.field("game").getValue().orElse(null);
					Game game = artTypeStr == null ? null : Game.valueOf(gameStr);
					if (game == Game.HS) {
						artForm = factory.form(BaseHearthstoneDeckArticle.class).bindFromRequest();
					} else if (game == Game.GWENT) {
						artForm = factory.form(BaseGwentDeckArticle.class).bindFromRequest();
					}

					loadDeck((BaseDeckArticle) artForm.get(),artForm);
				} else if (artType == ArtType.TIERDECK) {
					String gameStr = form.field("game").getValue().orElse(null);
					Game game = artTypeStr == null ? null : Game.valueOf(gameStr);
					if (game == Game.HS) {
						artForm = factory.form(BaseHSLegendDeck.class).bindFromRequest();
					} 
					loadDeck((BaseDeckArticle) artForm.get(),artForm);
				} else {
					return redirect("/api/edit");
				}


				art = artForm.get();

				String content = artForm.field("content2").getValue().orElse(null);
				if (content != null) {
					art.content=content;
				}
				boolean doEdit = Boolean.valueOf(artForm.field("updateEdit").getValue().orElse("false"));
				art.published = Boolean.valueOf(artForm.field("published").getValue().orElse("false"));;
				art.updateEdit = doEdit;
				art.editDate = new Date();
				boolean doFeature =  form.field("featured").getValue().isPresent();
				List<Long> rec = getRecommendedFromForm(form);
				List<Long> sim = getSimilarFromForm(form);
				if (art.id == -1) {
					art = artRep.add(art,rec,sim).toCompletableFuture().join();
					if (doFeature)
						featuredRepository.editID(art.id).toCompletableFuture().join();
					String loc = art instanceof BaseDeckArticle ? "tierlist/_" : "articles/";
					return Results.found("https://www.f2k.gg/" + loc + art.id);
				} else {
					if (doFeature)
						featuredRepository.editID(art.id).toCompletableFuture().join();
					art = artRep.edit(art,rec,sim).toCompletableFuture().join();
					String loc = art instanceof BaseDeckArticle ? "tierlist/_" : "articles/";
					return Results.found("https://www.f2k.gg/" + loc + art.id);
				}
			} else {
				return redirect("/api/users/login?url=" + request().uri());
			}
		},ec.current());
	}

	private List<Long> getRecommendedFromForm(DynamicForm form) {
		ArrayList<Long> res = new ArrayList<>();
		for (int i = 0; i < 6; i++){
			String val = form.field("recommended2["+i+"].id").getValue().orElse("");
			if (val != null && !val.equals("")) {
				res.add(Long.parseLong(val));
			}
		}
		return res;
	}

	private List<Long> getSimilarFromForm(DynamicForm form) {
		ArrayList<Long> res = new ArrayList<>();
		for (int i = 0; i < 6; i++){
			String val = form.field("similar2["+i+"].id").getValue().orElse("");
			if (val != null && !val.equals("")) {
				res.add(Long.parseLong(val));
			}
		}
		return res;
	}


	private void loadDeck(BaseDeckArticle art, Form<? extends BaseArticle> artForm) {
		int highestDeck = 0;
		for (int i = 0; i < 6; i++) {
			if (artForm.field("decks[" + i +"].name").getValue().orElse("").trim().equals("") && artForm.field("decks[" + i +"].cards[0].dbId").getValue().orElse("").trim().equals("")) {
				highestDeck = i-1;
				break;
			}
		}
		int[] deckSizes = new int[highestDeck+1];
		for (int i = 0; i < highestDeck+1; i++) {
			for (int j = 0; j < 31; j++) {
				if (artForm.field("decks[" + i +"].cards[" + j +"].dbId").getValue().orElse("").trim().equals("")) {
					deckSizes[i] = j-1;
					break;
				}
			}
		}

		List<DeckList<?>> decks = new ArrayList<>();
		if (art instanceof BaseHearthstoneDeckArticle){
			for (int i = 0; i <= highestDeck; i++) {

				HSDeckList dec =  new HSDeckList();
				List<BaseHearthstoneCard> cards = new ArrayList<>();
				dec.name = artForm.field("decks[" + i +"].name").getValue().get().trim();
				dec.heroClass = Hero.valueOf(artForm.field("decks[" + i +"].heroClass").getValue().get().trim());
				dec.mode = Mode.valueOf(artForm.field("decks[" + i +"].mode").getValue().get().trim());
				for (int j =0; j <= deckSizes[i]; j++) {
					BaseHearthstoneCard card = new BaseHearthstoneCard();
					card.dbId = Long.parseLong(artForm.field("decks[" + i +"].cards[" + j +"].dbId").getValue().get());
					cards.add(card);
				}
				dec.setCards(cards);
				decks.add(dec);
			}
		} else {
			for (int i = 0; i <= highestDeck; i++) {

				DeckList<BaseGwentCard> dec =  new GwentDeckList();
				List<BaseGwentCard> cards = new ArrayList<>();
				dec.name = artForm.field("decks[" + i +"].name").getValue().get().trim();
				for (int j =0; j <= deckSizes[i]; j++) {
					BaseGwentCard card = new BaseGwentCard();
					card.dbId = Long.parseLong(artForm.field("decks[" + i +"].cards[" + j +"].dbId").getValue().get());
					cards.add(card);
				}
				dec.setCards(cards);
				decks.add(dec);
			}
		}


		art.setDecks(decks);
	}



	private void filterYoutubeTwitch(Article art) {
		String img = art.imageURL;
		if (img.toLowerCase().contains("youtube")) {
			Matcher m =youtube.matcher(img);
			if (m.matches()) {
				art.imageURL = "https://www.youtube.com/embed/" + m.group("code");
			}
		} if (img.toLowerCase().contains("twitch")) {
			art.imageURL = "https://clips.twitch.tv/embed?autoplay=0&clip=" + img.split("twitch\\.tv/")[1];

		}
	}


	@play.filters.csrf.AddCSRFToken
	public CompletionStage<Result> editInsertArticle(Long id) {
		return userc.isLogged().thenApplyAsync(bool -> {
			if (bool) {
				ObjectMapper mapper = new ObjectMapper();
				return artRep.get(id).thenApplyAsync(art -> {
					art = art == null? new TextArticle() : art;
					java.util.stream.Stream<JsonNode> hsCards = cardRep.HSlist().toCompletableFuture().join().map(card -> {
						ObjectNode temp =  mapper.createObjectNode();
						temp.put("label",card.name)
						.put("hero",card.heroClass.name())
						.put("id",card.dbId)
						.put("cardID",card.cardId)
						.put("rarity",card.rarity.toString());
						return temp;
					});
					java.util.stream.Stream<JsonNode> gwCards = cardRep.GWlist().toCompletableFuture().join().map(card -> {
						ObjectNode temp =  mapper.createObjectNode();
						temp.put("label",card.name)
						.put("faction",card.faction.name())
						.put("id",card.dbId)
						.put("cardID",card.cardId);
						return temp;
					});
					if (art.getRecommended() != null) {
						((BaseArticle)art).getRecommended().forEach(a -> {
							a.setRecommended(null);
							if (a instanceof BaseDeckArticle) {
								((BaseDeckArticle<?>)a).setSimilar(null);
								((BaseDeckArticle<?>)a).setDecks(null);
								((BaseDeckArticle<?>)a).setSimilar(null);
							}
						});
					}
					if (art instanceof BaseDeckArticle) {
						((BaseDeckArticle<?>)art).getSimilar().forEach(a -> {
							a.setDecks(null);
							a.setRecommended(null);
							a.setSimilar(null);
						});
					}

					List<User> users = (List<User>) userRep.list(0, 200, null).toCompletableFuture().join().collect(Collectors.toList());
					List<JsonNode> hsList = hsCards.collect(Collectors.toList());
					List<JsonNode> gwList = gwCards.collect(Collectors.toList());
					return ok(generalindex.render(art,users,toJson(hsList),toJson(gwList)));
				},ec.current()).toCompletableFuture().join();
			} else {
				return redirect("/api/users/login?url=" + request().uri());
			}
		},ec.current());
	}

	public CompletionStage<Result> upvoteArticle(long id) {
		return artRep.upvote(id).thenApplyAsync(art -> {

			return ok(toJson(art));
		},ec.current());

	}


	public CompletionStage<Result> overlaySQR() {
		return adRepository.list().thenApplyAsync(ads -> {

			List<String> urls = ads.stream().map((ad) -> ad.imageSQR)
					.filter(imgSqr -> imgSqr != null && !imgSqr.equals(""))
					.collect(Collectors.toList());

			return ok(overlaySQR.render(toJson(urls)));
		},ec.current());
	}


	public CompletionStage<Result> overlayREC() {

		return adRepository.list().thenApplyAsync(ads -> {

			List<String> urls = ads.stream().map((ad) -> ad.imageRECT)
					.filter(imgRect -> imgRect != null && !imgRect.equals(""))
					.collect(Collectors.toList());

			return ok(overlayREC.render(toJson(urls)));
		},ec.current());
	}

	public CompletionStage<Result> jsonREC() {

		return adRepository.list().thenApplyAsync(ads -> {

			List<String> urls = ads.stream().map((ad) -> ad.imageRECT)
					.filter(imgRect -> imgRect != null && !imgRect.equals(""))
					.collect(Collectors.toList());

			return ok(toJson(urls));
		},ec.current());
	}

	public CompletionStage<Result> jsonSQR() {

		return adRepository.list().thenApplyAsync(ads -> {

			List<String> urls = ads.stream().map((ad) -> ad.imageSQR)
					.filter(imagSQR -> imagSQR != null && !imagSQR.equals(""))
					.collect(Collectors.toList());

			return ok(toJson(urls));
		},ec.current());
	}



	@RequireCSRFCheck
	public  CompletionStage<Result> doEditAds() {
		Form<AdList> form = factory.form(AdList.class).bindFromRequest();
		Request r = play.mvc.Http.Context.current().request();
		return userc.isLogged().thenApplyAsync(bool -> {
			if (bool) {
				if (!form.hasErrors()) {
					AdList ads =  form.get();
					return adRepository.update(ads.getAds()).thenApplyAsync(ads2 -> {
						return ok(toJson(ads2));
					}, ec.current()).toCompletableFuture().join();


				} else {
					return  ok(toJson(new ImmutablePair<>("errors", form.errorsAsJson())));
				}
			} else {
				return redirect("/api/users/login?url=" + request().uri());
			}
		},ec.current());
	}

	@play.filters.csrf.AddCSRFToken
	public CompletionStage<Result> editAds() {
		return userc.isLogged().thenApplyAsync(bool -> {
			if (bool) {
				return adRepository.list().thenApplyAsync(ads -> {

					return ok(advertisementIndex.render(ads));
				},ec.current()).toCompletableFuture().join();
			} else {
				return redirect("/api/users/login?url=" + request().uri());
			}
		},ec.current());
	}

}
