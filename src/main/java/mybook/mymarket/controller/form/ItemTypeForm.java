package mybook.mymarket.controller.form;


import lombok.Getter;

@Getter
public enum ItemTypeForm {
    Novel("소설"), Magazine("잡지"), Reference("참고서");

    private final String description;

    ItemTypeForm(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
