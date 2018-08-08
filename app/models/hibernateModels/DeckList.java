package models.hibernateModels;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.BatchSize;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
public abstract class DeckList<D extends BaseCard> {
	
	@Id
    @GeneratedValue(strategy = GenerationType.TABLE,generator="deckGen")
	@TableGenerator(name="deckGen",pkColumnValue="deck",allocationSize=6)
	public Long id;
	
	public String name;
	
	@JsonIgnore
	@Column(insertable=false,updatable=false)
	public long basedeck_id;
    
    @ManyToOne(fetch=FetchType.LAZY,cascade={CascadeType.MERGE,CascadeType.PERSIST})
    @JoinColumn(name="basedeck_id", nullable=true)
    @JsonIgnore
    public BaseDeckArticle<?> baseDeckArticle;

    @Transient
    public abstract List<D> getCards();
    

    @Transient
    public abstract void setCards(List<D> cards) ;
    
    
    
    
}
