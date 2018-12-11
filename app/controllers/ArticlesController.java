package controllers;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static play.libs.Json.toJson;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorSystem;
import akka.actor.Scheduler;
import models.AdRepository;
import models.Article;
import models.ArticleFeatured.Target;
import models.ArticleRepository;
import models.FeaturedRepository;
import models.UserRepository;
import models.filters.ArticleFilters;
import models.hibernateModels.BaseArticle;
import models.hibernateModels.BaseDeckArticle;
import play.data.Form;
import play.data.FormFactory;
import play.filters.csrf.RequireCSRFCheck;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import scala.concurrent.ExecutionContext;
import scala.concurrent.ExecutionContextExecutor;
import views.html.advertisementIndex;

/**
 * The controller for the aeticles API
 */
@Singleton
public class ArticlesController extends Controller {

	private final ActorSystem actorSystem;
	private final ExecutionContextExecutor exec;
	private final ArticleRepository articleRepository;
	private final models.hibernateModels.ArticleRepository artRep2;
	private final AdRepository adRepository;
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
			ArticleRepository articleRepository,
			AdRepository adRepository,HttpExecutionContext ec,
			UserController u,WSClient ws,UserRepository userRep,FeaturedRepository featuredRepository
			,models.hibernateModels.ArticleRepository rep2
			) {
		this.actorSystem = actorSystem;
		this.exec = exec;
		this.articleRepository = articleRepository;
		this.ec = ec;
		ArticlesController.ws = ws;
		this.featuredRepository = featuredRepository;
		this.userRep = userRep;
		this.userc = u;
		this.adRepository = adRepository;
		this.artRep2 =rep2;
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
			if (art != null && art.author != null) {
				art.author.hashpw = null;
			}
			if (art != null) {
				art.getRecommended().forEach(a -> {
					a.setRecommended(null);
					if (a instanceof BaseDeckArticle) {
						((BaseDeckArticle<?>)a).setDecks(null);
						((BaseDeckArticle<?>)a).setRecommended(null);
						((BaseDeckArticle<?>)a).setSimilar(null);
					}
				});
//				System.out.println(toJson(  ((BaseDeckArticle)art).getDecks()  )  );
				if (art instanceof BaseDeckArticle) {

					((BaseDeckArticle<?>)art).getSimilar().forEach(a -> {
						a.setDecks(null);
						a.setRecommended(null);
						a.setSimilar(null);
					});
				}
			}
			return ok(toJson(art));
		});
	}

	public CompletionStage<Result> getArticles(int offset,int amount,String type,String game) {
		try {
			enums.General.TextType type2 = type == null || type.equals("")? null : enums.General.TextType.valueOf(type);
			enums.General.Game game2 = game == null || game.equals("")? null : enums.General.Game.valueOf(game);
			return artRep2.list(offset,amount,enums.General.ArtType.ARTICLE, type2,game2, 0, null, false).thenApplyAsync(artStream -> {
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
			return ok((JsonNode)ArticleFilters.noNothing.valueToTree(artStream));
		}, ec.current());
	}

	public CompletionStage<Result> getFeatured() {
		return featuredRepository.list().thenApplyAsync(artStream -> {
			//artStream = artStream.map((art) -> {	art.author.hashpw = null; return art;});
			return ok((JsonNode)ArticleFilters.noNothing.valueToTree(artStream.map(art -> {
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
			e.printStackTrace();
		}
		return arts.stream();
	}


	@Inject FormFactory factory;
	//	@RequireCSRFCheck
	//	public  CompletionStage<Result> doEditInsertArticle() {
	//		Form<Article> form = factory.form(Article.class).bindFromRequest();
	//		boolean doFeature =  form.field("featured").getValue().isPresent();
	//		Request r = play.mvc.Http.Context.current().request();
	//		return userc.isLogged().thenApplyAsync(bool -> {
	//			if (bool) {
	//				if (!form.hasErrors()) {
	//					Article art =  form.get();
	//					TextArticle art2 = new TextArticle();
	//					art2.authorID = art.author.id;
	//					art2.content= art.content;
	//					art2.editDate = art.editDate;
	//					art2.imageURL = art.imageURL;
	//					art2.published = art.published;
	//					art2.rating= art.rating;
	//					art2.title= art.title;
	//					art2.game = enums.General.Game.valueOf(art.game.toString());
	//					art2.type = enums.General.TextType.valueOf(art.type.toString());
	//
	//					filterYoutubeTwitch(art);
	//					if (art.id == -1) {
	//						return artRep2.add(art2).thenApplyAsync(art3 -> {
	//							if (doFeature) {
	//								featuredRepository.editID(art3.id).toCompletableFuture().join();
	//							}
	//							return Results.found("http://f2k.gg/articles/_" + art3.id);
	//						}, ec.current()).toCompletableFuture().join();
	//					} else {
	//						art2.id = art.id;
	//						return artRep2.edit(art2).thenApplyAsync(art3 -> {	
	//							if (doFeature) {
	//								featuredRepository.editID(art3.id).toCompletableFuture().join();
	//							}
	//							return Results.found("http://d2416peknw0o5h.cloudfront.net/articles/_" + art3.id);
	//						}, ec.current()).toCompletableFuture().join();
	//					}
	//
	//
	//				} else {
	//					return  ok(toJson(new ImmutablePair<>("errors", form.errorsAsJson())));
	//				}
	//			} else {
	//				return redirect("/api/users/login?url=" + request().uri());
	//			}
	//		},ec.current());
	//	}
	//
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
	//
	//
	//	@play.filters.csrf.AddCSRFToken
	//	public CompletionStage<Result> editInsertArticle(Long id) {
	//		return userc.isLogged().thenApplyAsync(bool -> {
	//			if (bool) {
	//				return artRep2.get(id).thenApplyAsync(art -> {
	//
	//					TextArticle art2 = art == null ? new TextArticle() : (TextArticle)art;
	//
	//					List<User> users = (List<User>) userRep.list(0, 200, null).toCompletableFuture().join().collect(Collectors.toList());
	//					return ok(articleIndex.render(art2,users));
	//				},ec.current()).toCompletableFuture().join();
	//			} else {
	//				return redirect("/api/users/login?url=" + request().uri());
	//			}
	//		},ec.current());
	//	}
	//
	public CompletionStage<Result> upvoteArticle(long id) {
		return artRep2.upvote(id).thenApplyAsync(art -> {

			return ok(toJson(art));
		},ec.current());

	}


	public CompletionStage<Result> overlaySQR() {
		return adRepository.list().thenApplyAsync(ads -> {

			List<String> urls = ads.stream().map((ad) -> ad.imageSQR)
					.filter(imgSqr -> imgSqr != null && !imgSqr.equals(""))
					.collect(Collectors.toList());

			return ok(views.html.overlaySQR.render(toJson(urls)));
		},ec.current());
	}


	public CompletionStage<Result> overlayREC() {

		return adRepository.list().thenApplyAsync(ads -> {

			List<String> urls = ads.stream().map((ad) -> ad.imageRECT)
					.filter(imgRect -> imgRect != null && !imgRect.equals(""))
					.collect(Collectors.toList());

			return ok(views.html.overlayREC.render(toJson(urls)));
		},ec.current());
	}
	
	public CompletionStage<Result> overlaySQRTP() {
		return adRepository.list().thenApplyAsync(ads -> {

			List<String> urls = ads.stream().map((ad) -> ad.imageSQRTP)
					.filter(imgSqr -> imgSqr != null && !imgSqr.equals(""))
					.collect(Collectors.toList());

			return ok(views.html.overlaySQRTP.render(toJson(urls)));
		},ec.current());
	}
	
	public CompletionStage<Result> overlaySQRTPSlow() {
		return adRepository.list().thenApplyAsync(ads -> {

			List<String> urls = ads.stream().map((ad) -> ad.imageSQRTP)
					.filter(imgSqr -> imgSqr != null && !imgSqr.equals(""))
					.collect(Collectors.toList());

			return ok(views.html.overlaySQRTPSlow.render(toJson(urls)));
		},ec.current());
	}


	public CompletionStage<Result> overlayRECTP() {

		return adRepository.list().thenApplyAsync(ads -> {

			List<String> urls = ads.stream().map((ad) -> ad.imageRECTTP)
					.filter(imgRect -> imgRect != null && !imgRect.equals(""))
					.collect(Collectors.toList());

			return ok(views.html.overlayRECTP.render(toJson(urls)));
		},ec.current());
	}
	
	public CompletionStage<Result> overlayRECTPSlow() {

		return adRepository.list().thenApplyAsync(ads -> {

			List<String> urls = ads.stream().map((ad) -> ad.imageRECTTP)
					.filter(imgRect -> imgRect != null && !imgRect.equals(""))
					.collect(Collectors.toList());

			return ok(views.html.overlayRECTPSlow.render(toJson(urls)));
		},ec.current());
	}

	public CompletionStage<Result> overlayWOW() {

		return adRepository.list().thenApplyAsync(ads -> {

			List<String> urls = ads.stream().map((ad) -> ad.imageWOW)
					.filter(imgRect -> imgRect != null && !imgRect.equals(""))
					.collect(Collectors.toList());

			return ok(views.html.overlayWOW.render(toJson(urls)));
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
	public CompletionStage<Result> jsonRECTP() {

		return adRepository.list().thenApplyAsync(ads -> {

			List<String> urls = ads.stream().map((ad) -> ad.imageRECTTP)
					.filter(imgRect -> imgRect != null && !imgRect.equals(""))
					.collect(Collectors.toList());

			return ok(toJson(urls));
		},ec.current());
	}

	public CompletionStage<Result> jsonSQRTP() {

		return adRepository.list().thenApplyAsync(ads -> {

			List<String> urls = ads.stream().map((ad) -> ad.imageSQRTP)
					.filter(imagSQR -> imagSQR != null && !imagSQR.equals(""))
					.collect(Collectors.toList());

			return ok(toJson(urls));
		},ec.current());
	}
	
	public CompletionStage<Result> jsonWOW() {

		return adRepository.list().thenApplyAsync(ads -> {

			List<String> urls = ads.stream().map((ad) -> ad.imageWOW)
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

					System.out.println(toJson(ads));
					return ok(advertisementIndex.render(ads));
				},ec.current()).toCompletableFuture().join();
			} else {
				return redirect("/api/users/login?url=" + request().uri());
			}
		},ec.current());
	}
}

//	public CompletionStage<Result> doBeta(Long id) {
//		return articleRepository.list(0, 10000, null, null).thenApplyAsync(arts -> {
//			List<BaseArticle> newArts = artRep2.list(0, 10000,enums.General.ArtType.ARTICLE,null, null, 0, null, false).toCompletableFuture().join().collect(Collectors.toList());
//			System.out.println(toJson(newArts));
//			if (newArts.size() != 0) {
//				System.out.println("EXISTS");
//				return ok(toJson(newArts));
//			}
//			ObjectMapper mapper = new ObjectMapper();
//			ArrayNode json = mapper.createArrayNode();
//
//			arts.forEach(art -> {
//				TextArticle art2 = new TextArticle();
//				art2.id = art.id;
//				art2.authorID = art.authorID;
//				art2.content= art.content;
//				art2.editDate = art.editDate;
//				art2.imageURL = art.imageURL;
//				art2.published = art.published;
//				art2.rating= art.rating;
//				art2.title= art.title;
//				art2.game = enums.General.Game.valueOf(art.game.toString());
//				art2.type = enums.General.TextType.valueOf(art.type.toString());
//				json.add(toJson(artRep2.add(art2).toCompletableFuture().join()));
//			});
//			return ok(json);
//		},ec.current());
//	}




