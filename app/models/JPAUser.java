package models;


import play.db.jpa.JPAApi;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.ArticlesController;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Provide JPA operations running inside of a thread pool sized to the connection pool
 */
public class JPAUser implements UserRepository  {

	private final JPAApi jpaApi;
	private final DatabaseExecutionContext executionContext;

	@Inject
	public JPAUser(JPAApi jpaApi, DatabaseExecutionContext executionContext) {
		this.jpaApi = jpaApi;
		this.executionContext = executionContext;
	}

	@Override
	public CompletionStage<User> get(long id) {
		return supplyAsync(() -> wrap(em -> getByID(em,id)), executionContext);
	}

	public CompletionStage<User> add(User us) {
		return supplyAsync(() -> wrap(em -> insert(em, us)), executionContext);
	}

	public CompletionStage<Stream<User>> list(int offset,int amount,Boolean isOnline) {
		return supplyAsync(() -> wrap(em -> list(em,offset,amount,isOnline)), executionContext);
	}

	@Override
	public CompletionStage<User> getByName(String username) {
		return supplyAsync(() -> wrap(em -> getByName(em,username)), executionContext);
	}

	@Override
	public CompletionStage<User> edit(User u) {
		return supplyAsync(() -> wrap(em -> edit(em,u)),executionContext);
	}


	public User edit(EntityManager em, User u) {
		u = em.merge(u);
		return u;
	}

	private <T> T wrap(Function<EntityManager, T> function) {
		return jpaApi.withTransaction(function);
	}

	private User insert(EntityManager em, User us) {
		em.persist(us);
		return us;
	}

	private Stream<User> list(EntityManager em,int offset,int amount,Boolean isOnline) {
		TypedQuery<User> q= em.createQuery("select a from User a " + (isOnline == null? "" : "WHERE twitch IS NOT NULL"), User.class);
		List<User> u = q.setMaxResults(amount)
				.setFirstResult(offset)
				.getResultList();
	

		
		u = loadTwitch(u,isOnline);
		u.sort((a,b) -> a.twitchData != null? -1 : b.twitchData != null ? 1 : 0);
		return u.stream();
	}

	private User getByID(EntityManager em,long id) {
		User u = em.find(User.class, id);
		u.loadTwitch();
		return u;
	}
	
	
	public static List<User> loadTwitch(List<User> u,Boolean isOnline) {
		WSRequest req = ArticlesController.ws.url("https://api.twitch.tv/kraken/streams");
		StringBuilder channels = new StringBuilder();
		u.forEach(user -> {
			if (user.twitch != null)
				channels.append(user.twitch).append(",");
		}) ;

		if (channels.length() <1)
			return u;
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
						u.forEach(user -> {
							if (current.get("channel").get("name").asText().equals(user.twitch)) {
								user.twitchData = current;
							}
						});
							
						
					}
					if (isOnline != null)
						u.removeIf(user -> (user.twitchData == null) == isOnline.booleanValue());
				}
				return u;
			}).toCompletableFuture().get();
		} catch (InterruptedException | ExecutionException e) {
			return u;
		}
	}

	private User getByName(EntityManager em, String username) {
		TypedQuery<User> q= em.createQuery("select a from User a where username = :user", User.class);
		q.setParameter("user", username);
		return q.getSingleResult();
	}




}
