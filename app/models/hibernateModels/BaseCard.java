package models.hibernateModels;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonInclude;

import models.Card.Faction;
import models.Card.Hero;
import models.Card.Race;
import models.Card.Rarity;
import models.Card.Set;
import models.Card.Type;
import play.data.validation.Constraints.Required;

@Entity(name="BaseCard")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
public abstract class BaseCard implements Serializable {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name="db_id")
	public Long dbId;

	@Required
    public String name;

    public String text;

    public String artist;

    public String flavor;

    public String cardId;
    

	
}