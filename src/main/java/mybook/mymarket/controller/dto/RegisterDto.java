package mybook.mymarket.controller.dto;

import lombok.Getter;
import mybook.mymarket.domain.Register;
import mybook.mymarket.domain.RegisterStatus;

import java.time.LocalDateTime;


@Getter
public class RegisterDto {
    private Long registerId;    // 등록 id
    private Long memberId;      // 등록한 회원 id
    private Long itemId;        // 등록된 상품 id
    private String nickName;    // 등록한 회원 이름
    private String itemName;    // 등록된 상품 이름
    private String type;
    private int price;  // 등록된 상품 가격
    private int stockQuantity;  // 등록된 상품 수량
    private LocalDateTime registerDate; // 등록 날짜
    private RegisterStatus status;  // 등록 상태

    public RegisterDto(Register register) { // 파라미터 생성자, 엔티티 -> DTO
        this.registerId = register.getId();
        this.memberId = register.getMember().getId();
        this.itemId = register.getItem().getId();
        this.nickName = register.getMember().getNickName();
        this.itemName = register.getItem().getName();
        this.price = register.getItem().getPrice();
        this.stockQuantity = register.getItem().getStockQuantity();
        this.registerDate = register.getRegisterDate();
        this.status = register.getStatus();
    }
}
