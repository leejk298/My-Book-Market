package mybook.mymarket.domain.item;

import lombok.Getter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("참고서")
@Getter
public class Reference extends Item {   // 참고서
    private String subject; // 과목

    // Setter
    public void setSubject(String etc) {
        this.subject = etc;
    }
}
