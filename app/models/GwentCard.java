package models;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ArrayNode;

import play.data.validation.Constraints.Required;

@Entity(name="GwentCard")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Table(name="cardsgwent")
public class GwentCard implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1802365342132393177L;

	public enum Faction { 
		NEUTRAL,MONSTERS,NILFGAARD,NORTHERN_REALMS,SCOIATAEL,SKELLIGE;
		
		@Override
		public String toString() {
			String name = this.name();
			name = name.substring(0,1) + name.substring(1).toLowerCase();
			name = name.replace('_', ' ').replace("coia", "coia'");
			
			return name;
			
		}
	}
	public enum Leader { 
		//monsters
		dagon,eredin,unseen_elder,
		//nilfgaard
		emhyr_var_emreis,john_calveit,morvran_voorhis,
		//northern
		foltest,henselt,radovid,
		//scoiatael
		brouver_hoog,eithne,francesca,
		//skellige
		crache_an_craite,harald_the_cripple,king_bran;
		
		@Override
		public String toString() {
			return this.name().replace("_","-");
			
		}
	}
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="db_id")
	public Long dbId;
	
	@Required
	@Column(name = "uuid")
    public String cardId;

	@Required
    public String name;

//	@Required
//	@Enumerated(EnumType.STRING)
//    public Rarity rarity;

	@Required
	@Column(name="categories")
	@JsonIgnore
    private String categories2;
	

	@Transient
	public List<String> categories;

	@Required
	@Column(name="positions")
	@JsonIgnore
    private String positions2;
	

	@Transient
	public List<String> positions;
	
	@PostLoad
	protected void setArrays() {

		ObjectMapper mapper = new ObjectMapper();
		try {
			ArrayNode actualObj = (ArrayNode) mapper.readValue(categories2, ArrayNode.class);
			categories = new ArrayList<String>();
			actualObj.forEach(str -> {
				categories.add(str.asText());
			});
			actualObj = (ArrayNode) mapper.readValue(positions2, ArrayNode.class);
			positions = new ArrayList<String>();
			actualObj.forEach(str -> {
				positions.add(str.asText());
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
//	ObjectMapper mapper = new ObjectMapper();
//	JsonNode actualObj = mapper.readValue("{\"k1\":\"v1\"}", JsonNode.class);
//	
//	@Required
//	@Enumerated(EnumType.STRING)
//    public Set set;
//
//	@Required
//	@Column(name = "heroClass")
//	@Enumerated(EnumType.STRING)
//    public Hero heroClass;

//    public Integer attack;
//
//    public Integer health;
//    
//    public Integer durability;
//
//    public Integer cost;
//
//    public String text;


	
}