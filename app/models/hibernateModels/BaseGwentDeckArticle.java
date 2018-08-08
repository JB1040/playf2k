package models.hibernateModels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import enums.General.Difficulty;
import enums.Gwent.Faction;
import enums.Gwent.Leader;
import enums.Hearthstone.Set;
import models.Card;
import models.User;
import models.Card.Hero;
import models.DeckArticle.Mode;

@Entity(name="BaseGwentDeckArticle")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Table(name="basegwentdeckarticle")
public class BaseGwentDeckArticle extends BaseDeckArticle<GwentDeckList> {

	
	@Enumerated(EnumType.STRING)
	@Transient
	public Faction faction;
	

	@Enumerated(EnumType.STRING)
	@Transient
	public Leader leader;
	
	public  BaseGwentDeckArticle(long id,String title, String content,User author, Collection<DeckList> decks,Difficulty difficulty,Date date, Date editDate) {
		super(id, title, content, author, decks, difficulty, date, editDate);
	}
	
	@PostLoad
	private void setHeroAndMode() {
		List<GwentDeckList> theDecks = getDecks();
		if (theDecks.size() > 0) {
			GwentDeckList main = theDecks.get(0);
			this.faction=main.faction;
			this.leader = main.leader;
		}
	}
	
	@Override
	public List<GwentDeckList> getDecks() {
		List<GwentDeckList> res = new ArrayList<>(30);
		decks.forEach((d) -> res.add((GwentDeckList) d)); 
		return res;
	}
}
