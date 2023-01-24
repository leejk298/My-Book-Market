package mybook.mymarket.domain.item;

import lombok.Getter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("잡지")
@Getter
public class Magazine extends Item {    // 잡지
    private String theme; // 테마

    // Setter
    public void setTheme(String etc) {
        this.theme = etc;
    }
}
