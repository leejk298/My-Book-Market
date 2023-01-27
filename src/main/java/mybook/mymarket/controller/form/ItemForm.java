package mybook.mymarket.controller.form;


import lombok.Getter;
import lombok.Setter;


import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Getter @Setter
public class ItemForm {
    @NotEmpty
    private String name;        // 책이름
    @NotEmpty
    private String author;      // 저자
    @NotNull
    private int price;          // 판매가격
    @NotNull
    private int stockQuantity;  // 수량
    private ItemTypeForm itemTypeForm;  // Novel, Magazine, Reference
    private String etc;
}
