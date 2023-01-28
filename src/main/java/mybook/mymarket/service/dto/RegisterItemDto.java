package mybook.mymarket.service.dto;


import lombok.Getter;

@Getter
public class RegisterItemDto {
    private String name;
    private String author;
    private int price;
    private int stockQuantity;
    private String type;
    private String etc;

    /**
     * DTO 는 로직을 갖고있지 않는 순수한 데이터 객체이며 일반적으로 getter/setter 메소드만을 가진다.
     * 하지만 DTO 는 단순히 데이트를 옮기는 용도이기때문에 굳이 setter 를 이용해 값을 수정할 필요가 없고,
     * 생성자만을 사용하여 값을 할당하는 게 좋다.
     */
    protected RegisterItemDto() {
    }

    public RegisterItemDto(String name, String author, int price, int stockQuantity, String type, String etc) {
        this.name = name;
        this.author = author;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.type = type;
        this.etc = etc;
    }
}
