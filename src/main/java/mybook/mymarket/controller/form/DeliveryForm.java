package mybook.mymarket.controller.form;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor
public class DeliveryForm {     // 배송 유형
    private String code;    // 반환 값
    private String displayName; // 표시 이름
}
