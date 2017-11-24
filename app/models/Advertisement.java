package models;

import java.util.Date;

import javax.persistence.*;

import models.Article.ArtType;
import play.data.validation.Constraints.Required;

@Entity
@Table(name="advertisement")
public class Article {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;
    
    @Required
    public String name;

    @Required
    public String imageSQR;
    
    @Required
    public String imageRECT;

    
    @Enumerated(EnumType.STRING)
    public Game game;

  
}