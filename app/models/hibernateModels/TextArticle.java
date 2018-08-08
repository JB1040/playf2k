package models.hibernateModels;

import java.util.Date;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import enums.General.ArtType;
import enums.General.Game;
import enums.General.TextType;
import play.data.validation.Constraints.Required;

@Entity
@Table(name="basetextarticle")
public class TextArticle extends BaseArticle {
	

	

    @Required
    public String imageURL;
    
    @Enumerated(EnumType.STRING)
    public TextType type;
    
    @JsonIgnore
    @Transient
    public TextType getTheType() {
    	return type;
    }

    @Enumerated(EnumType.STRING)
    public Game game;
}
