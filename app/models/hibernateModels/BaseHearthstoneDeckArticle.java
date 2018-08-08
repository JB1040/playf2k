package models.hibernateModels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Polymorphism;
import org.hibernate.annotations.PolymorphismType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import enums.General.Difficulty;
import enums.Hearthstone.Hero;
import enums.Hearthstone.Mode;
import models.User;
@Entity(name="BaseHearthstoneDeckArticle")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Table(name="basehearthstonedeckarticle")
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
@Polymorphism(type = PolymorphismType.EXPLICIT)
@org.hibernate.annotations.Entity(polymorphism = PolymorphismType.EXPLICIT)
public class BaseHearthstoneDeckArticle extends BaseDeckArticle<HSDeckList> {

	public  BaseHearthstoneDeckArticle() {
	}

	public  BaseHearthstoneDeckArticle(long id,String title, String content, User author,Collection<DeckList> decks,  Difficulty difficulty,Date date, Date editDate) {
		super(id, title, content,author, decks, difficulty, date, editDate);
	}



	@Enumerated(EnumType.STRING)
	@JsonInclude
	@Column(name="hero_class")
	public Hero heroClass;


	@Enumerated(EnumType.STRING)
	@JsonInclude
	public Mode mode;


	@Column(nullable=true)
	public Boolean isStandard;
	
	@Column(name="importance",nullable=true)
	@JsonIgnore
	public Integer importance;
	
	@PostLoad
	private void setHeroAndMode() {
		if ( isStandard == null) {
			List<HSDeckList> theDecks = getDecks();
			if (theDecks.size() > 0) {
				HSDeckList main = theDecks.get(0);

				this.heroClass = main.heroClass;
				this.mode = main.mode;
				this.isStandard = main.isStandard;
			}
		}
	}


	@Override
	public List<HSDeckList> getDecks() {
		List<HSDeckList> res = new ArrayList<>(30);
		if( decks == null)
			decks = new ArrayList<>();
		decks.forEach((d) -> res.add((HSDeckList) d)); 
		return res;
	}
}
