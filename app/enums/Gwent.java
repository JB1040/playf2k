package enums;

import com.fasterxml.jackson.annotation.JsonFormat;

import models.GwentCard.Faction;

public class Gwent {
	public enum Group { BRONZE,SILVER,GOLD,LEADER}
	
	public enum Rarity { COMMON(30),RARE(80),EPIC(200),LEGENDARY(800);
		public int cost;
		
		Rarity(int cost) {
			this.cost = cost;
		}
	}

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
	
	
}
