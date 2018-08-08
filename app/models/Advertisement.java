package models;

import java.util.Date;

import javax.persistence.*;

import models.Article.ArtType;
import models.Article.Game;
import play.data.validation.Constraints.Required;

@Entity
@Table(name="advertisement")
public class Advertisement {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;
    
    @Required
    public String name;

    public String imageSQR;
    
    public String imageRECT;

    @Enumerated(EnumType.STRING)
    public Game game;

  
}