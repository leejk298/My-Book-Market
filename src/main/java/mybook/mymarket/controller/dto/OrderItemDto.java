package mybook.mymarket.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import mybook.mymarket.domain.OrderItem;

@Getter
public class OrderItemDto {
    @JsonIgnore
    private Long orderItemOrderId;  // 주문상품에 대한 주문 id
    private Long registerMemberId;  // 주문상품에 대한 등록한 회원 id
    private Long itemId;    // 주문상품에 대한 상품 id
    private String itemName;    // 주문상품에 대한 상품 이름
    private String registerMemberName;  // 주문상품에 대한 등록한 회원 이름
    private int count;  // 주문상품 수량
    private int price;  // 주문상품 금액

    public OrderItemDto(OrderItem orderItem) {  // Parameter Constructor
        this.orderItemOrderId = orderItem.getOrder().getId();
        this.registerMemberId = orderItem.getItem().getRegister().getMember().getId();
        this.itemId = orderItem.getItem().getId();
        this.itemName = orderItem.getItem().getName();
        this.registerMemberName = orderItem.getItem().getRegister().getMember().getNickName();
        this.count = orderItem.getCount();
        this.price = orderItem.getOrderPrice();
    }

    /**
     * Proxy 초기화
     * => Hibernate 에서 ProxyMember 객체를 상속받아둠 == ByteBuddyInterceptor (Proxy 기술)
     * => 실질적으로 Member 객체를 사용할 때 DB 에 SQL 을 날려서 가져와서 채워줌 => Proxy 초기화
     * 해결방안: Json 라이브러리 보고 Lazy 로딩인 것은 뿌리지말라고 명령 => Hibernate5Module, 라이브러리 필요
     * 근본적으로 엔티티를 전부 노출시킴 => 나중에 엔티티가 바뀌게되면 API 스펙이 바뀌게 됨 => 장애발생
     * 성능상으로도 문제 => 필요없는 OrderItem 까지 끌고오면서 Item, Category 까지 전부 딸려오는 쿼리가 나감
     * 결과적으로 엔티티를 직접 노출하는 것은 굉장히 안좋음 => 절대 X => DTO 로 변환해서 하자
     */
}
