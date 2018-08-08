package models.hibernateModels;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.NotFound;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import enums.Hearthstone.*;
import models.Card;
import models.Card.Hero;
import play.data.validation.Constraints.Required;

@Entity(name="BaseHearthstoneCard")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Table(name="basehearthstonecard")
public class BaseHearthstoneCard extends BaseCard{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1079205979592041606L;

	@Required
	@Enumerated(EnumType.STRING)
    public Rarity rarity;

	@Required
	@Enumerated(EnumType.STRING)
    public Type type;
	
	@Required
	@Enumerated(EnumType.STRING)
	@Column(name="sets")
    public Set set;

	@Enumerated(EnumType.STRING)
	public Hero heroClass;

    public Integer attack;

    public Integer health;
    
    public Integer durability;

    public Integer cost;
    
    @Enumerated(EnumType.STRING)
    public Race race;

	@Enumerated(EnumType.STRING)
    public Faction faction;

    public boolean elite;

	@Column(name="how_to_earn")
    public String howToEarn;
}
