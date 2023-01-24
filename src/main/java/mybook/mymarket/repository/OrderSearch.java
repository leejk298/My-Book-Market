package mybook.mymarket.repository;


import lombok.Getter;
import lombok.Setter;
import mybook.mymarket.domain.DealStatus;
import mybook.mymarket.domain.OrderStatus;


@Getter @Setter
public class OrderSearch {
    /**
     * where 문에서 검색될 조건들
     */
    private String nickName;     // 회원 닉네임
    private DealStatus dealStatus; // 거래 상태 [WAIT, COMP]
    private OrderStatus orderStatus;    // 주문 상태 [ORDER, CANCEL]
}
