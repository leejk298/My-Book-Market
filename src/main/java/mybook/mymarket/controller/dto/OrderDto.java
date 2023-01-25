package mybook.mymarket.controller.dto;


import lombok.Getter;
import mybook.mymarket.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter // No serializer, no properties 에러 => @Getter 필요 (=> @Data (포괄적))
public class OrderDto {
    private Long orderId;
    private Long memberId;      // 주문한 회원 id
    private String orderMemberName;    // 주문한 회원 이름
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;    //  주문 상태
    private DealStatus dealStatus;  //  거래 상태
    private DealType dealType;
    private Address address;
    private List<OrderItemDto> orderItems;  //  주문 상품
    /**
     * private List<OrderItem> orderItems; // 추가
     * DTO 안에 엔티티가 필드로 오면 안됨 (매핑 <OrderItem>도 포함)
     * 왜냐하면 API 스펙으로 엔티티가 외부에 그대로 노출됨
     * => 완전히 엔티티에 대한 의존을 끊어야함
     * => OrderItemDto 내부 클래스로 엔티티 -> DTO 로 변환시켜야함
     * => 온전히 엔티티가 아닌 DTO 로 API 스펙이 나가는 것
     */

    // Dto 가 파라미터로 엔티티를 받는 것은 문제가 안됨
    // 왜냐하면 중요하지 않은 곳에서 중요한 엔티티를 의존하기때문에
    public OrderDto(Order order) {  // 파라미터 생성자
        this.orderId = order.getId();
        this.memberId = order.getMember().getId();      // Proxy 초기화
        this.orderMemberName = order.getMember().getNickName();    // Proxy 초기화
        this.orderDate = order.getOrderDate();
        this.orderStatus = order.getStatus();
        this.dealStatus = order.getDeal().getStatus();  // Proxy 초기화
        this.dealType = order.getDeal().getType();
        this.address = order.getDeal().getAddress();
        // OrderItem 은 엔티티여서 안나옴 => Lazy 강제 초기화 필요 => Proxy 초기화
        // order.getOrderItems().stream().forEach(o -> o.getItem().getName());
        // this.orderItems = order.getOrderItems();
        this.orderItems = order.getOrderItems().stream()
                .map(orderItem -> new OrderItemDto(orderItem))
                .collect(Collectors.toList());
    }
}
