package controllers;

import akka.actor.ActorSystem;
import javax.inject.*;
import javax.swing.text.html.HTML;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.typesafe.play.cachecontrol.ResponseServeActions.Validate;

import akka.actor.Scheduler;
import models.Article;
import models.Article.Game;
import models.ArticleRepository;
import models.JPAArticle;
import models.User;
import models.UserRepository;
import play.libs.concurrent.HttpExecutionContext;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.Constraints.Required;
import play.data.validation.Constraints.Validatable;
import play.filters.csrf.CSRF;
import play.filters.csrf.RequireCSRFCheck;
import play.libs.concurrent.HttpExecution;
import play.mvc.*;
import play.mvc.Http.Cookie;

import java.util.concurrent.Executor;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
public class UserController extends Controller {


	public static final String MAINURL = "/";

	private final ActorSystem actorSystem;
	private final ExecutionContextExecutor exec;
	public static  UserRepository userRepository;
	private final HttpExecutionContext ec;


	/**
	 * @param actorSystem We need the {@link ActorSystem}'s
	 * {@link Scheduler} to run code after a delay.
	 * @param exec We need a Java {@link Executor} to apply the result
	 * of the {@link CompletableFuture} and a Scala
	 * {@link ExecutionContext} so we can use the Akka {@link Scheduler}.
	 * An {@link ExecutionContextExecutor} implements both interfaces.
	 */
	@Inject
	public UserController(ActorSystem actorSystem, ExecutionContextExecutor exec,
			UserRepository userRepository, HttpExecutionContext ec) {
		this.actorSystem = actorSystem;
		this.exec = exec;
		this.userRepository = userRepository;
		this.ec = ec;
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

	public CompletionStage<Result> getUser(long id) {
		return userRepository.get(id).thenApplyAsync(art -> {
			return ok(toJson(art));
		}, ec.current());
	}

	public CompletionStage<Result> getUsers(int offset,int amount,String online) {

		try {
			Boolean isOnline = online == null? null : Boolean.valueOf(online);
			
			return userRepository.list(offset,amount,isOnline).thenApplyAsync(artStream -> {
				return ok(toJson(artStream.collect(Collectors.toList())));
			}, ec.current());
		} catch (IllegalArgumentException e) {
			return supplyAsync(() -> ok(toJson(new ImmutablePair<>("error", "Could not parse parameters."))));
		}
	}

	public CompletionStage<Boolean> isLogged() {

		Cookie cookie = request().cookie("id");
		if (cookie == null) {
			return supplyAsync(() -> {
				return false;
			},ec.current());
		}

		return userRepository.get(Long.parseLong(cookie.value())).thenApplyAsync(u -> {
			return u != null && u.code !=null && u.code.equals(request().cookie("PLAY_SESSION").value()) ;
		},ec.current()) ;

	}


	@Inject FormFactory factory;

	@play.filters.csrf.AddCSRFToken
	public CompletionStage<Result> login(String url) {
		Login log = new Login();
		log.previous = url == null? null : url;
		return supplyAsync(() ->  ok(views.html.login.f().apply(factory.form(Login.class).fill(log))),ec.current());
	}

	@RequireCSRFCheck
	public CompletionStage<Result> doLogin() {
		Form<Login> loginForm = factory.form(Login.class).bindFromRequest();
		return supplyAsync(() -> {
			if (loginForm.hasErrors()) {
				return badRequest(views.html.login.f().apply(loginForm));
			} else {
				response().setCookie(Cookie.builder("id", loginForm.get().id +"").build());
				userRepository.get(loginForm.get().id).thenApplyAsync(us -> {
					us.code = request().cookie("PLAY_SESSION").value();
					return us;
				},ec.current()).thenApplyAsync(u ->  {
					try {
						User temp = userRepository.edit(u).toCompletableFuture().get();
						return temp;
					} catch (InterruptedException | ExecutionException e1) {
					}
					return null;
				} ,ec.current()).thenApplyAsync(u2 -> { 
					return u2;
				});

				return redirect(loginForm.get().previous);
			}
		},ec.current());
	}

	@play.data.validation.Constraints.Validate
	public static class Login implements Validatable<String> {


		@Inject private UserRepository u = UserController.userRepository	;

		public Long id;

		@Required
		public String username;
		@Required
		public String password;

		public String previous;


		public String validate() {
			try {
				return u.getByName(username).thenApply(user -> {
					if (user.hashpw.equals(password) && user.username.equalsIgnoreCase(username)) {
						id = user.id;
						return null;
					} else {
						return "Incorrect login.";
					}
				}).toCompletableFuture().get();
			} catch (Exception e) {
				return "Incorrect login.";
			}
		}



	}


}
