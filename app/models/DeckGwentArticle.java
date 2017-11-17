package models;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.persistence.*;
import javax.validation.constraints.Size;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.WhereJoinTable;
import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import controllers.ArticlesController;
import models.Article.Game;
import models.Card.Hero;
import models.GwentCard.Faction;
import models.GwentCard.Leader;
import play.data.validation.Constraints.Required;
import play.data.validation.Constraints.Validate;
import play.data.validation.ValidationError;
import play.libs.ws.WSResponse;

import static play.libs.Json.toJson;

@Entity
@Table(name="deck_gwent_article")
@Validate
@DynamicUpdate
public class DeckGwentArticle implements play.data.validation.Constraints.Validatable<List<ValidationError>> {

	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="id")
	public Long id;

	@ManyToOne(fetch=FetchType.EAGER,cascade=CascadeType.PERSIST)
	@Required
	@JoinColumn(name="author_id")
	public User author;

	@Required
	public String title;

	

	@BatchSize(size=30)
	@ManyToMany(fetch = FetchType.EAGER,cascade=CascadeType.PERSIST)
	@JoinTable(
			name="gwent_card",
			joinColumns=@JoinColumn(name="deck_id", referencedColumnName="id"),
			inverseJoinColumns=@JoinColumn(name="card_id", referencedColumnName="db_id"))
	public List<GwentCard> cards;

	@Required
	public String content;

	@Enumerated(EnumType.STRING)
	public Game game;

	public boolean published;

	public int rating;
	public int tier;


	@Column(insertable=false,updatable=false,nullable=true)
	public Date date;
	
    @Column(name="editDate",insertable=false,updatable=true,nullable=false)
    public Date editDate;

	@Enumerated(EnumType.STRING)
	@Column(name="faction")
	public Faction faction;
	

	@Enumerated(EnumType.STRING)
	@Column(name="leader")
	public Leader leader;

	@Override
	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<>();
		return errors;
	}


}