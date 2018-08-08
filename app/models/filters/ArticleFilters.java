package models.filters;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import models.hibernateModels.BaseArticle;
import models.hibernateModels.BaseDeckArticle;
import models.hibernateModels.DeckList;
import models.hibernateModels.HSDeckList;

public final class ArticleFilters {
	public static final ObjectMapper noRecommended =  new ObjectMapper();
	public static final ObjectMapper noRecommendedNoSimilar =  new ObjectMapper();
	public static final ObjectMapper noNothing =  new ObjectMapper();
	static {
		noRecommended.registerModule(new Jdk8Module());
		noRecommended.registerModule(new JavaTimeModule());
		noRecommended.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		noRecommended.addMixIn(BaseArticle.class, withoutRecommended.class);
		noRecommendedNoSimilar.registerModule(new Jdk8Module());
		noRecommendedNoSimilar.registerModule(new JavaTimeModule());
		noRecommendedNoSimilar.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		noRecommendedNoSimilar.addMixIn(BaseDeckArticle.class, withoutRecommendedSimilar.class);
		noNothing.registerModule(new Jdk8Module());
		noNothing.registerModule(new JavaTimeModule());
		noNothing.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		noNothing.addMixIn(BaseArticle.class, NoNothing.class);
	}
	
    public abstract class withoutRecommended {
    	@JsonIgnore public abstract Set<BaseArticle> getRecommended();
    	@JsonIgnore public abstract Set<BaseDeckArticle<?>> getSimilar();
    }
    public abstract class withoutRecommendedSimilar {
    	@JsonIgnore public abstract Set<BaseArticle> getRecommended();
    	@JsonIgnore public abstract Set<BaseDeckArticle<?>> getSimilar();
    }

    public abstract class NoNothing {
    	@JsonIgnore public abstract Set<BaseArticle> getRecommended();
    	@JsonIgnore public abstract Set<BaseDeckArticle<?>> getSimilar();
    	@JsonIgnore public abstract <E extends DeckList> List<E > getDecks();
    	@JsonIgnore public abstract HSDeckList getDeck();
    }
}
