package models.hibernateModels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import enums.Gwent.*;
import play.data.validation.Constraints.Required;


@Entity(name="BaseGwentCard")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Table(name="basegwentcard")
public class BaseGwentCard extends BaseCard {
	
	public int strength;
	
	@Enumerated(EnumType.STRING)
	public Faction faction;

	@Enumerated(EnumType.STRING)
	public Rarity rarity;
	
	@Enumerated(EnumType.STRING)
	@Column(name="groups")
	public Group group;
	
	@Convert(converter = StringListConverter.class)
	public List<String> categories;

	@Convert(converter = StringListConverter.class)
	public List<String> positions;
	
	
}
