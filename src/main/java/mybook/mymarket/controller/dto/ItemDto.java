package mybook.mymarket.controller.dto;


import lombok.Getter;
import mybook.mymarket.controller.form.ItemForm;
import mybook.mymarket.domain.item.Item;

@Getter
public class ItemDto {
    private Long id;    // id
    private String name;    // 이름
    private String author;  // 저자
    private int price;  // 가격
    private int stockQuantity;  // 재고
    private ItemTypeDto type;    // 분류
    private String etc;     // 추가 정보

    // 엔티티 -> DTO
    public ItemDto(Item item) { // 파라미터 생성자
        id = item.getId();
        name = item.getName();
        author = item.getAuthor();
        price = item.getPrice();
        stockQuantity = item.getStockQuantity();
    }

    // Form -> DTO
    public ItemDto(ItemForm form) { // 파라미터 생성자
        id = form.getId();
        name = form.getName();
        author = form.getAuthor();
        price = form.getPrice();
        stockQuantity = form.getStockQuantity();
        etc = form.getEtc();
    }

    // Setter
    public void setType(ItemTypeDto itemTypeDto) {
        this.type = itemTypeDto;
    }
}
