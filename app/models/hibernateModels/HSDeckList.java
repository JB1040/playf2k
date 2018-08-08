package models.hibernateModels;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.BatchSize;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import enums.Hearthstone.Hero;
import enums.Hearthstone.Mode;
import enums.Hearthstone.Set;
import utils.HearthstoneUtils;

@Entity
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Table(name="hsdecklist")
public class HSDeckList extends DeckList<BaseHearthstoneCard> {
	
	@BatchSize(size=30)
	@ManyToMany(fetch = FetchType.EAGER)
	@ElementCollection(targetClass=BaseHearthstoneCard.class)
	@JoinTable(
			name="hsdeckcard",
			joinColumns=@JoinColumn(name="deck_id", referencedColumnName="id"),
			inverseJoinColumns=@JoinColumn(name="card_id", referencedColumnName="db_id"))
    protected List<BaseHearthstoneCard> cards;

	@Override	
	@Transient
	public List<BaseHearthstoneCard> getCards() {
		return cards;
	}
	
	@Override	
	@Transient
	public void setCards(List<BaseHearthstoneCard> cards) {
		this.cards = cards;
	}

	@Enumerated(EnumType.STRING)
	@JsonProperty("heroClass")
	public Hero heroClass;

	
	@Enumerated(EnumType.STRING)
	public Mode mode;


	static List<Set> stdSets = Arrays.asList(new Set[] { 
			Set.CORE, Set.EXPERT1,
			Set.UNGORO, Set.ICECROWN,Set.LOOTAPALOOZA, Set.GILNEAS, Set.BOOMSDAY});

	@Transient
	public boolean isStandard;
	
	@PostLoad
	public void setStandardAndCode() {
		if (mode == Mode.BRAWL) {
			isStandard = false;
		} else if (mode == Mode.ARENA) {
			isStandard = true;
		} else {
			boolean stand = true;
			if (cards != null && stand) {
				for (BaseHearthstoneCard c: cards) {
					if (c.set != null && !stdSets.contains(c.set)) {
						stand = false;
						break;
					}
				}
			}
			isStandard=stand;
		}
		
	}
	
	public String code;


	


}
