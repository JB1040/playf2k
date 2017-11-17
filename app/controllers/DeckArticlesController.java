package controllers;

import akka.actor.ActorSystem;
import javax.inject.*;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
import models.GwentCardRepository;
import models.JPAArticle;
import models.User;
import models.UserRepository;
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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import controllers.routes;
import scala.collection.immutable.Stream;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import views.html.deckIndex;
import scala.concurrent.ExecutionContextExecutor;

import static play.libs.Json.toJson;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * The controller for the aeticles API
 */
@Singleton
public class DeckArticlesController extends Controller {

	private final ActorSystem actorSystem;
	private final ExecutionContextExecutor exec;
	private final DeckArticleRepository deckArticleRepository;
	private final CardRepository cardRepository;
	private final GwentCardRepository gwentCardRepository;
	private final HttpExecutionContext ec;
	private final UserController userc;
	private final UserRepository userRep;
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
			GwentCardRepository gwentCardRepository) {
		this.actorSystem = actorSystem;
		this.exec = exec;
		this.deckArticleRepository = deckArticleRepository;
		this.cardRepository = cardRepository;
		this.ec = ec;
		this.userc = userc;
		this.userRep=userRep;
		this.gwentCardRepository = gwentCardRepository;
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
		return deckArticleRepository.get(id).thenApplyAsync(art -> {
			art.author.hashpw = null;

			return ok(toJson(art));
		}, ec.current());
	}

	public static java.util.stream.Stream<DeckArticle> loadTwitch(List<DeckArticle> arts) {
		WSRequest req = ArticlesController.ws.url("https://api.twitch.tv/kraken/streams");
		StringBuilder channels = new StringBuilder();
		arts.forEach(art -> {
			if (art.author.twitch != null)
				channels.append(art.author.twitch).append(",");
		});
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return arts.stream();
	}
	
	public CompletionStage<Result> getArticles(int offset,int amount,int tier,String mode,String isStandard,String game) {

		try {
			Mode mode2 = mode == null? null : Mode.valueOf(mode);
			Boolean isStandard2 = isStandard == null? null : Boolean.valueOf(isStandard);
			Article.Game game2 = game == null || game.equals("")? null : Game.valueOf(game);
			return deckArticleRepository.list(offset,amount,tier,mode2,isStandard2,game2).thenApplyAsync(artStream -> {
				//artStream = artStream.map((art) -> {	art.author.hashpw = null; return art;});
				JsonNode data = toJson(loadTwitch(artStream.collect(Collectors.toList())));
				for (JsonNode deck : data) {
					if (deck.get(game).asText().equals(Game.GWENT.toString())) {
						 ((ObjectNode) deck).replace("cards", deck.get("cards2"));
					}
					 ((ObjectNode) deck).remove("cards2");
				}
				return ok();
			}, ec.current());
		} catch (IllegalArgumentException e) {
			return supplyAsync(() -> ok(toJson(new ImmutablePair<>("error", "Could not parse parameters."))));
		}
	}

	public CompletionStage<Result> byAuthor(long id) {
		return deckArticleRepository.byAuthor(id).thenApplyAsync(artStream -> {
			return ok(toJson(loadTwitch(artStream.collect(Collectors.toList()))));
		}, ec.current());
	}

	@Inject FormFactory factory;
	@RequireCSRFCheck
	public  CompletionStage<Result> doEditInsertArticle() {
		Form<DeckArticle> form = factory.form(DeckArticle.class).bindFromRequest();
		boolean updateTime = form.field("updateTime").getValue().isPresent();
		Request r = play.mvc.Http.Context.current().request();
		return userc.isLogged().thenApplyAsync(bool -> {
			if (bool) {
				if (!form.hasErrors()) {
					DeckArticle art =  form.get();
					if (updateTime) {
						art.editDate = new Date();
					}
					if (art.id == -1) {
						return deckArticleRepository.add(art).thenApplyAsync(art2 -> {

							return Results.found("http://d2416peknw0o5h.cloudfront.net/tier_list/_" + art2.id);
						}, ec.current()).toCompletableFuture().join();
					} else {
						return deckArticleRepository.edit(art).thenApplyAsync(art2 -> {
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
		public CompletionStage<Result> editInsertArticle(Long id) {
			return userc.isLogged().thenApplyAsync(bool -> {
				if (bool) {
					return (Result) cardRepository.list().thenApplyAsync(art -> {

						ArrayNode json = Json.newArray();
						art.forEach(obj -> {
							json.add(Json.newObject().put("label", (String) obj[0])
										.put("actualVal", (Long) obj[1])
										.put("cardID", (String) obj[2])
										.put("hero", ((Card.Hero) obj[3]).toString()));
						});
						return json;
					},ec.current()).thenCombineAsync(deckArticleRepository.get(id), (cards,art) -> {
						art = art == null ? new DeckArticle() : art;
						
						List<User> users = (List<User>) userRep.list(0, 200, null).toCompletableFuture().join().collect(Collectors.toList());
						return ok(deckIndex.render(art,toJson(cards),users));
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
				return ok(toJson(art));
			},ec.current());
		}

	}
