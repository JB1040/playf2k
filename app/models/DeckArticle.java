package models;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.persistence.*;
import javax.validation.constraints.Size;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.WhereJoinTable;
import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import controllers.ArticlesController;
import models.Article.Game;
import models.Card.Hero;
import play.data.validation.Constraints.Required;
import play.data.validation.Constraints.Validate;
import play.data.validation.ValidationError;
import play.libs.ws.WSResponse;

import static play.libs.Json.toJson;

@Entity
@Table(name="deck_article")
@Validate
@DynamicUpdate
public class DeckArticle implements play.data.validation.Constraints.Validatable<List<ValidationError>> {

	public enum Mode {CON,ARENA,BRAWL};
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="id")
	public Long id;
	

	@Column(name="author_id",insertable=false,updatable=false)
	public long authorID;

	@ManyToOne(fetch=FetchType.EAGER,cascade=CascadeType.PERSIST)
	@Required
	@JoinColumn(name="author_id")
	public User author;

	@Required
	public String title;


	@BatchSize(size=30)
	@ManyToMany(cascade=CascadeType.PERSIST)
	@JoinTable(
			name="deck_card",
			joinColumns=@JoinColumn(name="deck_id", referencedColumnName="id"),
			inverseJoinColumns=@JoinColumn(name="card_id", referencedColumnName="db_id"))
	public List<Card> cards;
	
	@Required
	public String content;

	@Enumerated(EnumType.STRING)
	public Game game;

	public boolean published;

	public int rating;
	public int tier;


	@Column(insertable=false,updatable=false,nullable=true)
	public Date date;
	
    @Column(name="editDate",insertable=false,updatable=true,nullable=false)
    public Date editDate;

	@Enumerated(EnumType.STRING)
	@Transient
	@JsonProperty("heroClass")
	public Hero heroClass2;

	@ManyToOne(optional = true,fetch=FetchType.EAGER,cascade=CascadeType.PERSIST)
	@JsonIgnore
	@JoinColumn(name="heroClass",nullable=true)
	@NotFound 
	public Card heroCard;

	@Enumerated(EnumType.STRING)
	public Mode mode;

	@Transient
	public boolean isStandard;


	@Transient
	public String code;

	@PostLoad
	protected void setStandard() {
		boolean stand = mode == Mode.CON;
		if (cards != null && stand) {
			for (Card c: cards) {
				if (c.set != Card.Set.OG && c.set != Card.Set.KARA && 
						c.set != Card.Set.GANGS && c.set != Card.Set.UNGORO &&
						c.set != Card.Set.CORE && c.set != Card.Set.EXPERT1 &&
						c.set != Card.Set.ICECROWN && c.set != Card.Set.LOOTAPALOOZA) {
					stand = false;
					break;
				}
			}
		}
		isStandard=stand;

		setCode();
	}

	protected void setCode() {
		this.heroClass2 = heroCard == null ? Hero.NEUTRAL : heroCard.heroClass;
		ByteArrayOutputStream baos = null;
		DataOutputStream dos = null;
		if (heroCard != null) {
			try {
				baos = new ByteArrayOutputStream();
				dos = new DataOutputStream(baos);

				writeVarInt(dos, 0); // always zero
				writeVarInt(dos, 1); // encoding version number
				writeVarInt(dos, isStandard ? 2 : 1); // standard = 2, wild = 1
				writeVarInt(dos, 1); // number of heroes in heroes array, always 1
				writeVarInt(dos, heroCard.dbId.intValue()); // DBF ID of hero
				List<List<Integer>> deckOrdered = orderedDeck();

				int length = deckOrdered.get(0).size();
				writeVarInt(dos, length); // number of 1-quantity cards

				for (int i = 0; i < length; i++) {
					writeVarInt(dos, deckOrdered.get(0).get(i));
				}

				length = deckOrdered.get(1).size();
				writeVarInt(dos, length); // number of 1-quantity cards

				for (int i = 0; i < length; i++) {
					writeVarInt(dos, deckOrdered.get(1).get(i));
				}

				for (int i = 2; i < deckOrdered.size()-2; i++) {

				}
				writeVarInt(dos, 0); //the number of cards that have quantity greater than 2. Always 0 for constructed

				dos.flush(); //flushes the output stream to the byte array output stream

				if (baos != null)
					baos.close();
				if (dos != null)
					dos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			String deckString = Base64.getEncoder().encodeToString(baos.toByteArray()); //encode the byte array to a base64 string
			this.code=deckString;
		}
	}
	private List<List<Integer>> orderedDeck() {
		List<List<Integer>> res = new ArrayList<>();
		res.add(new ArrayList<>());
		res.add(new ArrayList<>());
		for (Card c : cards) {
			int id = c.dbId.intValue();
			for (int i = 0; i < res.size(); i++) {
				if (res.get(i).contains(id)) {
					res.get(i).remove((Object) id);
					if (i + 1 == res.size()) {
						res.add(new ArrayList<>());
					}
					res.get(i+1).add(id);
					break;
				} else if (i +1 == res.size()) {
					res.get(0).add(id);
				}
			}

		}
		return res;
	}

	@Override
	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<>();
		//if (cards != null && cards.size() != 30) {
		//	errors.add(new ValidationError("","A deck needs 30 cards."));
		//}
		return errors;
	}




	private  void writeVarInt(DataOutputStream dos, int value) throws IOException {
		//taken from http://wiki.vg/Data_types, altered slightly
		do {
			byte temp = (byte) (value & 0b01111111);
			
			// Note: >>> means that the sign bit is shifted with the rest of the
			// number rather than being left alone
			value >>>= 7;
				if (value != 0) {
					temp |= 0b10000000;
				}
				dos.writeByte(temp);
		} while (value != 0);
	}


}