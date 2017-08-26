package models;

import java.util.Date;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;

import controllers.ArticlesController;
import models.Article.Game;
import play.data.validation.Constraints.Required;
import play.libs.ws.WSResponse;

@Entity
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Table(name="user")
public class User {
	
	public enum UserType {
		ADMIN,STREAMER,CAPTAIN,ANALYST,CONTENT_CREATOR,CONTENT_MANAGER,MANAGER,HEADPUBLIC("Head of public relations"),DEVELOPER;
		private String name = null;
	
		private UserType() {}
		private UserType(String n) {name = n;}
		
		public String toString() {
			if (name == null) {
				return this.name().charAt(0) + name().substring(1).replace('_', ' ');
			} else {
				return name;
			}
		}
	}

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;
	
	@Required
    public String username;
    
	@Required
	@Basic(fetch = FetchType.LAZY)
	@JsonIgnore
    public String hashpw;
	
	@Basic(fetch = FetchType.LAZY)
	@JsonIgnore
    public String code;
   
	@Required
    public String email;
	
	@Column(name="fullName")
	public String fullName;
	
	public String residence;
	
	public Date birthday;
	
	public String description;
	
	@Column(name = "image")
	public String imageURL;
	// SOCIALS

	public String facebook;
	
	public String twitter;
	
	public String twitch;
		
	public String youtube;
	

    @Column(insertable=false)
    public Date createDate;
    
    
    @Enumerated(EnumType.STRING)
    public Game game;

    @Enumerated(EnumType.STRING)
    public UserType type;
    
    @Transient
    public JsonNode twitchData;
    
  
    
	public void loadTwitch() {
        if (twitch != null) {
        	 CompletionStage<JsonNode> res = ArticlesController.ws.url("https://api.twitch.tv/kraken/streams")
        		.addQueryParameter("channel", twitch.toLowerCase())
        		.addHeader("Client-ID", "oki24o1s3i52q86ullp92hh1c4wzk9x")
        		.get()
        		.thenApply(WSResponse::asJson);
        	 try {
				twitchData = res.toCompletableFuture().get().get("streams").get(0);
			} catch (InterruptedException | ExecutionException e) {
				
			} 
        }
    }
}