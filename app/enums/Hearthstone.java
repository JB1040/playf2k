package enums;

public class Hearthstone {
	public enum Rarity {
		FREE(0),COMMON(40),RARE(100),EPIC(400),LEGENDARY(1600);
		
		public int cost;
		
		private Rarity(int cost) { this.cost = cost;}
	}
	
	public enum Mode {CON,ARENA,BRAWL};
	
	public enum Type {MINION,SPELL,WEAPON,HERO,HEROPOWER}

	public enum Set {TGT,GANGS,CORE,UNGORO,EXPERT1,HOF,OG,BRM,GVG,KARA,LOE,NAXX,HERO_SKINS,ICECROWN,LOOTAPALOOZA,GILNEAS,BOOMSDAY}

	public enum Faction {HORDE,ALLIANCE}
	
	public enum Server {EU,NA,ASIA}

	public enum Race { BEAST,DEMON,ELEMENTAL,MURLOC,MECHANICAL,DRAGON,PIRATE,TOTEM,ALL}
	public enum Hero { 
		NEUTRAL(0),WARRIOR(7),PRIEST(813),WARLOCK(893),SHAMAN(1066),HUNTER(31),ROGUE(930),PALADIN(671),MAGE(637),DRUID(274);
		
		public int dbId;
		
		Hero(int id) {
			this.dbId=id;
		}
	}
}
