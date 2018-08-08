package models.hibernateModels;


import java.util.ArrayList;
import java.util.Collection;

import static play.libs.Json.toJson;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
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
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Polymorphism;
import org.hibernate.annotations.PolymorphismType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import enums.General.Difficulty;
import enums.General.Game;
import enums.Hearthstone.Server;
import models.User;

@Entity(name="BaseHSLegendDeck")
@Table(name="basehslegenddeck")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@AttributeOverrides({
	@AttributeOverride(name = "tier", column = @Column(name="tier", insertable = false, updatable = false)),
	@AttributeOverride(name = "content", column = @Column(name="content", insertable = false, updatable = false)),
	@AttributeOverride(name = "difficulty", column = @Column(name="difficulty", insertable = false, updatable = false))
})
public class BaseHSLegendDeck extends BaseHearthstoneDeckArticle{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2783174465315409611L;

	public BaseHSLegendDeck() {
	}

	public  BaseHSLegendDeck(long id,String title, String content, User author,Collection<DeckList> decks,Difficulty difficulty,Date date, Date editDate) {
		super(id, title, content,author,decks,difficulty, date, editDate);
	}

	@Transient
	public HSDeckList deck;


	public int rank;

	@Column(name="editDate",insertable=false,updatable=true,nullable=false)
	public Date editDate;

	public String player;

	@Enumerated(EnumType.STRING)
	@JsonInclude
	public Server server;

	@Transient
	@JsonIgnore
	public int tier;

	@Transient
	@JsonIgnore
	public String content;


	@Transient
	@JsonIgnore
	public Difficulty difficulty;


	@Override
	@JsonIgnore
	public List<HSDeckList> getDecks() {
		List<HSDeckList> res = new ArrayList<>(30);
		if( decks == null) {
			decks = new ArrayList<>();
		}
		if (deck == null && decks != null && decks.size() >=1) {
			this.deck = (HSDeckList) decks.get(0);
			
		}

		this.decks.forEach((d) -> res.add((HSDeckList) d)); 
		return res;
	}

	@JsonIgnore
	public void setDecks(List<HSDeckList> decks) {
		if (decks != null) {
			List<DeckList> res = new ArrayList<>(1);
			decks.forEach((d) -> res.add(d)); 
			this.decks = res;

			if (decks.size() >=1) {
				this.deck = (HSDeckList) decks.get(0);
			}
		} else{
			this.setDeck(null);
		}
	}
	
	public void setDeck(HSDeckList deck) {
		this.deck = deck;
	}
	
	

	private void doSetDeck() {
		if (decks != null && decks.size() >=1 && deck == null) {
			this.deck = (HSDeckList) decks.get(0);
		}
	}

	@JsonInclude
	public HSDeckList getDeck() {
		return deck;
	}

	@JsonIgnore
	@Transient
	public Integer importance;
}

