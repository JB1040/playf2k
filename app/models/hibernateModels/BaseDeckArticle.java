package models.hibernateModels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Polymorphism;
import org.hibernate.annotations.PolymorphismType;

import com.fasterxml.jackson.annotation.JsonInclude;

import enums.General.Difficulty;
import enums.General.Game;
import models.User;

@Entity(name="BaseDeckArticle")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
@Polymorphism(type = PolymorphismType.EXPLICIT)
public abstract class BaseDeckArticle<D> extends BaseArticle {


	/**
	 * 
	 */
	private static final long serialVersionUID = 4728450085188628289L;

	@OneToMany(fetch=FetchType.LAZY,mappedBy="baseDeckArticle")
	@OrderBy("id ASC")
	protected List<DeckList> decks;

	public  BaseDeckArticle() {

	}
	public  BaseDeckArticle(long id,String title, String content, User author, Collection<DeckList> decks, Difficulty difficulty,Date date, Date editDate) {
		this.id = id;
		this.title = title;
		this.content = content;
		this.author = author;
		//this.decks = new ArrayList<DeckList>(decks);
		this.difficulty = difficulty;
		this.date = date;
		this.editDate = editDate;
	}

	public abstract <E extends DeckList> List<E > getDecks();

	public void setDecks(List<D> decks) {
		if (decks != null) {
			List<DeckList> res = new ArrayList<>(30);

			decks.forEach((d) -> res.add((DeckList)d)); 
			this.decks = res;
		} else {
			this.decks = null;
		}
		
	}

	public int tier;

	@Column(name="editDate",insertable=false,updatable=true,nullable=false)
	public Date editDate;

	@Transient
	@JsonInclude
	public Game game;

	@Column(name="craft_cost",nullable=true)
	public Integer cost;

	@ManyToMany(fetch = FetchType.LAZY,cascade={CascadeType.ALL})
	@JoinTable(name="similardeck",
	joinColumns={@JoinColumn(name="deckart_id")},
	inverseJoinColumns={@JoinColumn(name="similar_id")})
	@Size(min=0, max=6)
	@ElementCollection(targetClass=BaseDeckArticle.class)
	public Set<BaseDeckArticle<D>> similar;

	//	@ManyToMany(fetch = FetchType.EAGER,cascade={CascadeType.ALL})
	//	@JoinTable(name="similardeck",
	//		joinColumns={@JoinColumn(name="similar_id")},
	//		inverseJoinColumns={@JoinColumn(name="deckart_id")})
	//	@ElementCollection(targetClass=BaseDeckArticle.class)
	//	@JsonInclude
	//	 public Set<BaseDeckArticle<D>> inverseSimilar;

	public Set<BaseDeckArticle<D>> getSimilar() {
		return similar;
	}

	public void setSimilar(Set<BaseDeckArticle<D>> similar) {
		this.similar = similar;
	}

	//	public Set<BaseDeckArticle<D>> getInverseSimilar() {
	//		return inverseSimilar;
	//	}
	//	
	//	public void setInverseSimilar(Set<BaseDeckArticle<D>> inverseSimilar) {
	//		this.inverseSimilar = inverseSimilar;
	//	}

	@Enumerated(EnumType.STRING)
	public Difficulty difficulty;

	@PostLoad
	public void setGame() {
		if (this instanceof BaseHearthstoneDeckArticle || this instanceof BaseHSLegendDeck) {
			this.game = Game.HS;
		} else if (this instanceof BaseGwentDeckArticle) {
			this.game = Game.GWENT;
		} else {
			this.game = Game.OTHER;
		}
	}
}
