package mybook.mymarket.repository.register.query;


import lombok.Data;
import mybook.mymarket.domain.RegisterStatus;

import java.time.LocalDateTime;

@Data
public class RegisterQueryDto {
    private Long registerId;    // 등록 id
    private Long memberId;      // 등록한 회원 id
    private Long itemId;        // 등록된 상품 id
    private String nickName;    // 등록한 회원 이름
    private String itemName;    // 등록된 상품 이름
    private int price;  // 등록된 상품 가격
    private int stockQuantity;  // 등록된 상품 수량
    private LocalDateTime registerDate; // 등록 날짜
    private RegisterStatus status;  // 등록 상태

    public RegisterQueryDto (Long registerId, Long memberId, Long itemId, String nickName, String itemName,
                             int price, int stockQuantity, LocalDateTime registerDate, RegisterStatus status) {
        this.registerId = registerId;
        this.memberId = memberId;
        this.itemId = itemId;
        this.nickName = nickName;
        this.itemName = itemName;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.registerDate = registerDate;
        this.status = status;
    }
}
