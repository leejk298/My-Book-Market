package mybook.mymarket.controller.form;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Getter @Setter
public class ItemForm {
    private Long id;    // 상품수정이 있어서 id 필요

    private String name;        // 책이름

    private String author;      // 저자

    private int price;          // 판매가격

    private int stockQuantity;  // 수량

    private ItemTypeForm itemTypeForm;  // Novel, Magazine, Reference

    private String etc;
}
