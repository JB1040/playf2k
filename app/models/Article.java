package models;

import java.util.Date;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import models.Article.ArtType;
import play.data.validation.Constraints.Required;

@Entity
@Table(name="article")
public class Article {

	public enum Game{HS,GWENT};
	public enum ArtType{PODCASTS, HIGHLIGHTS, VIEWPOINTS, METAREPORTS, TEAMS,ANNOUNCEMENTS,CARD_REVEALS,GIVEAWAYS};
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;
	
	@Column(name="author_id",insertable=false,updatable=false)
	public long authorID;
	
    @ManyToOne(fetch=FetchType.EAGER,cascade=CascadeType.PERSIST)
    @Required
    @JoinColumn(name="author_id")
    public User author;
    
    @Required
    public String title;

    @Required
    public String imageURL;

    @Required
    public String content;
    
    @Enumerated(EnumType.STRING)
    public Game game;

    @Enumerated(EnumType.STRING)
    public ArtType type;
    
    @Transient
    @JsonIgnore
    public ArtType getTheType() {
    	return type;
    }
    
    
    public boolean published;
    
    public int rating;
    
    @Column(insertable=false,updatable=false)
    public Date date;
    
    @Column(insertable=false,updatable=false)
    public Date editDate;
    
	public ArtType getType() {
		return type;
	}
}