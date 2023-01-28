package mybook.mymarket.service;


import lombok.Getter;
import mybook.mymarket.controller.form.ItemForm;

@Getter
public class RegisterItemDto {
    private String name;
    private String author;
    private int price;
    private int stockQuantity;
    private String type;
    private String etc;

    public RegisterItemDto(ItemForm form) {
        name = form.getName();
        author = form.getAuthor();
        price = form.getPrice();
        stockQuantity = form.getStockQuantity();
        type = form.getItemTypeForm().name();
        etc = form.getEtc();
    }
}
