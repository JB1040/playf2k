package models;

import play.db.jpa.JPAApi;
import play.db.*;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

import static play.libs.Json.toJson;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Provide JPA operations running inside of a thread pool sized to the
 * connection pool
 */
public class JPAAd implements AdRepository {

	private final JPAApi jpaApi;
	private final DatabaseExecutionContext executionContext;

	@Inject
	public JPAAd(JPAApi jpaApi, DatabaseExecutionContext executionContext) {
		this.jpaApi = jpaApi;
		this.executionContext = executionContext;
	}

	public CompletionStage<List<Advertisement>> list() {
		return supplyAsync(() -> wrap(em -> list(em)), executionContext);
	}

	public CompletionStage<List<Advertisement>> update(List<Advertisement> ads) {
		return supplyAsync(() -> wrap(em -> update(em, ads)), executionContext);
	}

	private <T> T wrap(Function<EntityManager, T> function) {
		return jpaApi.withTransaction(function);
	}

	public List<Advertisement> update(EntityManager em, List<Advertisement> ads) {
		List<Advertisement> a = list(em);
		a.forEach(ad -> em.remove(ad));
		ads.forEach(ad -> em.persist(ad));
		return ads;
	}

	private List<Advertisement> list(EntityManager em) {
		TypedQuery<Advertisement> q = em.createQuery("select a from Advertisement a", Advertisement.class);
		return q.getResultList();
	}

}
