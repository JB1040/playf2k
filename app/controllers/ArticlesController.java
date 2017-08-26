package controllers;

import akka.actor.ActorSystem;
import javax.inject.*;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.Scheduler;
import models.Article;
import models.Article.ArtType;
import models.Article.Game;
import models.ArticleFeatured.Target;
import models.DeckArticle.Mode;
import models.ArticleRepository;
import models.FeaturedRepository;
import models.JPAArticle;
import models.User;
import models.UserRepository;
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
import scala.concurrent.ExecutionContextExecutor;
import static play.libs.Json.toJson;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * The controller for the aeticles API
 */
@Singleton
public class ArticlesController extends Controller {

	private final ActorSystem actorSystem;
	private final ExecutionContextExecutor exec;
	private final ArticleRepository articleRepository;
	private final HttpExecutionContext ec;
	private final UserController userc;
	private final UserRepository userRep;
	public static  WSClient ws;

	private final Pattern youtube = Pattern.compile("(https?://)?(www\\.)?(youtube\\.com/watch\\?.*?v=|youtu\\.be/)(?<code>[^&]*)", Pattern.CASE_INSENSITIVE);
	private  final FeaturedRepository featuredRepository;

	/**
	 * @param actorSystem We need the {@link ActorSystem}'s
	 * {@link Scheduler} to run code after a delay.
	 * @param exec We need a Java {@link Executor} to apply the result
	 * of the {@link CompletableFuture} and a Scala
	 * {@link ExecutionContext} so we can use the Akka {@link Scheduler}.
	 * An {@link ExecutionContextExecutor} implements both interfaces.
	 */
	@Inject
	public ArticlesController(ActorSystem actorSystem, ExecutionContextExecutor exec,
			ArticleRepository articleRepository, HttpExecutionContext ec,
			UserController u,WSClient ws,UserRepository userRep,FeaturedRepository featuredRepository) {
		this.actorSystem = actorSystem;
		this.exec = exec;
		this.articleRepository = articleRepository;
		this.ec = ec;
		ArticlesController.ws = ws;
		this.featuredRepository = featuredRepository;
		this.userRep = userRep;
		this.userc = u;
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
		return articleRepository.get(id).thenApplyAsync(art -> {
			art.author.hashpw = null;

			return ok(toJson(art));
		}, ec.current());
	}

	public CompletionStage<Result> getArticles(int offset,int amount,String type) {
		try {
			Article.ArtType type2 = type == null || type.equals("")? null : ArtType.valueOf(type);
			return articleRepository.list(offset,amount,type2).thenApplyAsync(artStream -> {

				return ok(toJson(loadTwitch(artStream.collect(Collectors.toList()))));
			}, ec.current());
		} catch (IllegalArgumentException e) {
			return supplyAsync(() -> ok(toJson(new ImmutablePair<>("error", "Could not parse parameters."))));
		}
	}

	public CompletionStage<Result> byAuthor(long id) {
		return articleRepository.byAuthor(id).thenApplyAsync(artStream -> {
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

	public static java.util.stream.Stream<Article> loadTwitch(List<Article> arts) {
		WSRequest req = ArticlesController.ws.url("https://api.twitch.tv/kraken/streams");
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return arts.stream();
	}


	@Inject FormFactory factory;
	@RequireCSRFCheck
	public  CompletionStage<Result> doEditInsertArticle() {
		Form<Article> form = factory.form(Article.class).bindFromRequest();
		Request r = play.mvc.Http.Context.current().request();
		return userc.isLogged().thenApplyAsync(bool -> {
			if (bool) {
				if (!form.hasErrors()) {
					Article art =  form.get();
					filterYoutubeTwitch(art);
					if (art.id == -1) {
						return articleRepository.add(art).thenApplyAsync(art2 -> {

							return Results.found("http://d2416peknw0o5h.cloudfront.net/#/articles/" + art2.id);
						}, ec.current()).toCompletableFuture().join();
					} else {
						return articleRepository.edit(art).thenApplyAsync(art2 -> {
							return Results.found("http://d2416peknw0o5h.cloudfront.net/#/articles/" + art2.id);
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
				return articleRepository.get(id).thenApplyAsync(art -> {

					art = art == null ? new Article() : art;

					List<User> users = (List<User>) userRep.list(0, 200, null).toCompletableFuture().join().collect(Collectors.toList());
					return ok(articleIndex.render(art,users));
				},ec.current()).toCompletableFuture().join();
			} else {
				return redirect("/api/users/login?url=" + request().uri());
			}
		},ec.current());
	}

	public CompletionStage<Result> upvoteArticle(long id) {
		return articleRepository.upvote(id).thenApplyAsync(art -> {

			return ok(toJson(art));
		},ec.current());

	}





}
