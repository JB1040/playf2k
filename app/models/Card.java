package models;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import play.data.validation.Constraints.Required;

@Entity(name="Card")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Table(name="cards")
public class Card implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1802365345132393177L;

	public enum Rarity {
		FREE(0),COMMON(40),RARE(100),EPIC(400),LEGENDARY(1600);
		
		public int cost;
		
		private Rarity(int cost) { this.cost = cost;}
	}
	
	public enum Type {MINION,SPELL,WEAPON,HERO}

	public enum Set {TGT,GANGS,CORE,UNGORO,EXPERT1,HOF,OG,BRM,GVG,KARA,LOE,NAXX,HERO_SKINS,ICECROWN}
	
	public enum Faction {HORDE,ALLIANCE}

	public enum Race { BEAST,DEMON,ELEMENTAL,MURLOC,MECHANICAL,DRAGON,PIRATE,TOTEM}
	public enum Hero { NEUTRAL,WARRIOR,PRIEST,WARLOCK,SHAMAN,HUNTER,ROGUE,PALADIN,MAGE,DRUID}
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="db_id")
	public Long dbId;
	
	@Required
	@Column(name = "id")
    public String cardId;

	@Required
    public String name;

	@Required
	@Enumerated(EnumType.STRING)
    public Rarity rarity;

	@Required
	@Enumerated(EnumType.STRING)
    public Type type;
	
	@Required
	@Enumerated(EnumType.STRING)
    public Set set;

	@Required
	@Column(name = "heroClass")
	@Enumerated(EnumType.STRING)
    public Hero heroClass;

    public Integer attack;

    public Integer health;
    
    public Integer durability;

    public Integer cost;

    public String text;

	@Enumerated(EnumType.STRING)
    public Race race;

	@Enumerated(EnumType.STRING)
    public Faction faction;

    public boolean elite;

	@Column(name="how_to_earn")
    public String howToEarn;

    public String artist;

    public String flavor;

	
}