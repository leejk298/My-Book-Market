package mybook.mymarket.repository.order.query;


import lombok.Data;
import mybook.mymarket.domain.Address;
import mybook.mymarket.domain.DealStatus;
import mybook.mymarket.domain.DealType;
import mybook.mymarket.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
// @EqualsAndHashCode(of = "orderId")  // v6에서 collect()할 때 orderId를 기준으로 묶어줌
public class OrderQueryDto {
    private Long orderId;
    private Long memberId;
    private String orderMemberName;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private DealStatus dealStatus;
    private DealType dealType;
    private Address address;
    private List<OrderItemQueryDto> orderItems;
    // DTO 안에 엔티티가 필드로 오면 안됨 (매핑 <OrderItem>도 포함)
    // 왜냐하면 API 스펙으로 엔티티가 외부에 그대로 노출됨
    // => 완전히 엔티티에 대한 의존을 끊어야함
    // => OrderItemDto 내부 클래스로 엔티티 -> DTO 로 변환시켜야함
    // => 온전히 엔티티가 아닌 DTO 로 API 스펙이 나가는 것

    public OrderQueryDto (Long orderId, Long memberId, String orderMemberName, LocalDateTime orderDate, OrderStatus orderStatus,
                          DealStatus dealStatus, DealType dealType, Address address) {
        this.orderId = orderId;
        this.memberId = memberId;
        this.orderMemberName = orderMemberName;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.dealStatus = dealStatus;
        this.dealType = dealType;
        this.address = address;
        // JPQL 을 짜더라도 new 키워드에 컬렉션을 바로 넣을 수 가 없음
        // this.orderItems = orderItems;
    }

}
