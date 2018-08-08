package models;

import java.util.Date;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import models.Article.ArtType;
import models.hibernateModels.BaseArticle;
import play.data.validation.Constraints.Required;

@Entity(name="ArticleFeatured")
@Table(name="article_featured")
public class ArticleFeatured {
	
	public enum Target{ARTICLE,DECK}
	public enum FeaturedType{FEATURED,SPONSORED}
	
	@Required
	@Column(name = "id")
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
	
	@OneToOne(fetch=FetchType.EAGER,cascade=CascadeType.PERSIST,optional=true)
    @JoinColumn(name="article_id")
    public BaseArticle article;
	
	@OneToOne(fetch=FetchType.EAGER,cascade=CascadeType.PERSIST,optional=true)
    @JoinColumn(name="deck_id")
    public DeckArticle deck;
	

	@Enumerated(EnumType.STRING)
	public Target target;   
	@Enumerated(EnumType.STRING)
	public FeaturedType type;
	
}