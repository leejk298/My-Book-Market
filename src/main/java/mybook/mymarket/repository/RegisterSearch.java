package mybook.mymarket.repository;


import lombok.Getter;
import lombok.Setter;
import mybook.mymarket.domain.RegisterStatus;

@Getter @Setter
public class RegisterSearch {
    /**
     * where 문에서 검색될 조건들
     */
    private String nickName;    // 등록한 사람
    private String itemName;    // 상품명
    private RegisterStatus registerStatus;  // 등록 상태 [REGISTER, CANCEL]
}
