package models;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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

	public enum Group { BRONZE,SILVER,GOLD,LEADER}
	

	public enum Rarity { COMMON,RARE,EPIC,LEGENDARY}

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
	
	
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum Leader {  
		neutral(null,Faction.NEUTRAL,""),
		//monsters
		Dagon(5,Faction.MONSTERS,"a8bdVQGjV6KaullcE8mc9Q"),
		Eredin(5,Faction.MONSTERS,"zeJ6DKvuWCKNLuhFd0X2WQ"),
		Unseen_Elder(5,Faction.MONSTERS,"syItPNBBUU6q5_MB1yaeDQ"),
		//nilfgaard
		Emhyr_var_Emreis(7,Faction.NILFGAARD,"l0iUO6eWWMSA7Z0jLWXRSw"),
		John_Calveit(4,Faction.NILFGAARD,"F8j_qbytWvmaHMQSpccDww"),
		Morvran_Voorhis(7,Faction.NILFGAARD,"AQkgzstLXZq9GwDdBheEuQ"),
		//northern
		Foltest(5,Faction.NORTHERN_REALMS,"Zk8arw13UJqR9A3Ego82XA"),
		Henselt(3,Faction.NORTHERN_REALMS,"lsF-j3LrVWyS01whktOyyg"),
		Radovid(6,Faction.NORTHERN_REALMS,"rtpI83axVde6EZoBmS50gw"),
		//scoiatael
		Brouver_Hoog(4,Faction.SCOIATAEL,"61CL3W4jXw-oi4bi5JIbjg"),
		Eithne(5,Faction.SCOIATAEL,"xTEJt1aCXxWo-of63YYWVg"),
		Francesca(7,Faction.SCOIATAEL,"5RiN5KiPW7KYOcyQxPX-pQ"),
		//skellige
		Crache_an_Craite(5,Faction.SKELLIGE,"0yevWlnyUP6GY9buxdNQWg"),
		Harald_the_Cripple(5,Faction.SKELLIGE,"QZioC31FWQCiwJfvIO9Bmw"),
		King_Bran(2,Faction.SKELLIGE,"HkUaGM14XBKpSFU4cV8Sww");
		
		public String name = this.toString();
		public Integer strength;
		public Faction faction;
		public String cardId;
		private Leader(Integer str,Faction fac,String id) {
			this.strength=str;
			this.faction = fac;
			this.cardId = id;
		}
		
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

	@Required
    public String text;

    public String flavor;
	

	
    public Integer strength;

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

	@Enumerated(EnumType.STRING)
	public Faction faction;

	@Enumerated(EnumType.STRING)
	public Rarity rarity;
	
	@Column(name="group2")
	@Enumerated(EnumType.STRING)
	public Group group;
	
	@PostLoad
	protected void setArrays() {

		ObjectMapper mapper = new ObjectMapper();
		try {
			ArrayNode actualObj = mapper.readValue(categories2, ArrayNode.class);
			categories = new ArrayList<String>();
			actualObj.forEach(str -> {
				categories.add(str.asText());
			});
			actualObj = mapper.readValue(positions2, ArrayNode.class);
			positions = new ArrayList<String>();
			actualObj.forEach(str -> {
				positions.add(str.asText());
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	class TestSerializer extends JsonSerializer<Enum> {
	    @Override
	    public void serialize(Enum value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
	        jgen.writeString(value.toString());
	    }
	}


	
}