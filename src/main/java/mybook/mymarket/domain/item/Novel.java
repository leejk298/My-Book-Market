package mybook.mymarket.domain.item;

import lombok.Getter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("소설")
@Getter
public class Novel extends Item {   // 소설
    private String genre;   // 장르

    // Setter
    public void setGenre(String etc) {
        this.genre = etc;
    }
}
