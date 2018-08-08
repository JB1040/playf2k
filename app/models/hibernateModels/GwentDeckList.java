package models.hibernateModels;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.BatchSize;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import enums.Gwent.Faction;
import enums.Gwent.Leader;

@Entity
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Table(name="gwentdecklist")
public class GwentDeckList extends DeckList<BaseGwentCard> {
	
	@BatchSize(size=30)
	@ManyToMany(fetch = FetchType.LAZY)
	@ElementCollection(targetClass=BaseGwentCard.class)
	@JoinTable(
			name="gwentdeckcard",
			joinColumns=@JoinColumn(name="deck_id", referencedColumnName="id"),
			inverseJoinColumns=@JoinColumn(name="card_id", referencedColumnName="db_id"))
    protected List<BaseGwentCard> cards;


	@Enumerated(EnumType.STRING)
	@Column(name="faction")
	@JsonIgnore
	public Faction faction;
	

	@Enumerated(EnumType.STRING)
	@Column(name="leader")
	public Leader leader;
	
	
	@JsonProperty("faction")
	public String factionString() {
	    return faction.toString();
	}

	
	@Override	
	@Transient
	public List<BaseGwentCard> getCards() {
		return cards;
	}
	
	@Override	
	@Transient
	public void setCards(List<BaseGwentCard> cards) {
		this.cards = cards;
	}



}
