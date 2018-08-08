package models.hibernateModels;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.*;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import models.User;
import models.Article.Game;
import play.data.validation.Constraints.Required;

@Entity
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
@JsonIdentityInfo(
		  generator = ObjectIdGenerators.PropertyGenerator.class, 
		  property = "id")
public abstract class BaseArticle  implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7057078120314914716L;

	@Id
    @GeneratedValue(strategy = GenerationType.TABLE)
	public Long id;

	@Column(name="author_id",insertable=false,updatable=false)
	public long authorID;
	
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="author_id")
    public User author;
    
    @Required
    public String title;
    
    public String url;
    
    public String imageURL;
    
    public String content;
    
    public boolean published;
    
    public int rating;
    
    @Column(insertable=true,updatable=false)
    @Temporal(TemporalType.TIMESTAMP)
    public Date date;
    
    @Column(name="editDate",insertable=true,updatable=true,nullable=false)
    @Temporal(TemporalType.TIMESTAMP)
    public Date editDate;
    
    @Transient
    @JsonIgnore
    public boolean updateEdit = false;

	@ManyToMany(fetch = FetchType.LAZY,cascade={CascadeType.ALL})
	@JoinTable(name="recommendedarticle",
		joinColumns={@JoinColumn(name="baseart_id")},
		inverseJoinColumns={@JoinColumn(name="recommended_id")})
	@Size(min=0, max=6)
	@ElementCollection(targetClass=BaseArticle.class)
    public Set<BaseArticle> recommended;
	
	public Set<BaseArticle> getRecommended() {
		return recommended;
	}
	
	public void setRecommended(Set<BaseArticle> recommended) {
		this.recommended = recommended;
	}
	

    
    @PrePersist
    protected void onCreate() {
    	if (editDate != null) {
    		date = (Date) editDate.clone();
    	} else {
    		date = new Date();
    	}
    }
    
    @PreUpdate
    protected void onUpdate() {
    	if (updateEdit)
    		editDate = new Date();
    }


    
}
