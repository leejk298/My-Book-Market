package mybook.mymarket.controller.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
public enum ItemTypeDto {
    Novel("소설"), Magazine("잡지"), Reference("참고서");

    private final String description;

    ItemTypeDto(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
