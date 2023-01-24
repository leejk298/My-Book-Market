package mybook.mymarket.domain;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.FetchType.LAZY;

@Entity
@Getter
@Table(name = "orders")
// protected Order() { } => 다른 곳에서 생성자를 제약시키고, 생성 메소드 이용하라고 알리기 위해
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA 에서 protected 는 생성자 사용하지 말라는 뜻
public class Order {
    @Id @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id") // 외래키 매핑
    private Member member;  // 연관관계의 주인(Order.member) => 외래키 갖고있는 N 쪽

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)   // 읽기전용, OrderItem.order 에 의해 매핑됨
    private List<OrderItem> orderItems = new ArrayList<>();
    // cascade: order 만 persist 해도 orderItem, delivery 가 같이 따라옴, delete 도 마찬가지

    @OneToOne(fetch = LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "deal_id")   // 외래키 매핑
    private Deal deal;  // 연관관계의 주인(Order.deal)

    // 컬럼명이 order_date 로 바뀜(카멜케이스)
    private LocalDateTime orderDate;    // 주문시간, Java8부터 하이버네이트가 지원

    @Enumerated(EnumType.STRING)    // 무조건 STRING 으로, ORDINAL 은 숫자 => 필드 추가 시 오류발생
    private OrderStatus status; // 주문상태 [ORDER, CANCEL]

    /**
     * 연관관계 메소드 **
     * - 양방향 연관관계일 때 양쪽에 값을 다 넣어줘야함
     * - 한 쪽만 세팅하고 까먹을 수 있으므로 원자적으로 묶어서 처리
     * - 위치는 컨트롤하는 쪽에 있는 것이 낫다
     */
    public void setMember(Member member) {  // Member <--> Order
        this.member = member;   // Order 에 member 세팅
        member.getOrders().add(this);   // Member 에 order 세팅
    }

    public void addOrderItem(OrderItem orderItem) { // OrderItem <--> Order
        this.orderItems.add(orderItem); // Order 에 orderItem 세팅
        orderItem.setOrder(this);   // OrderItem 에 order 세팅
    }

    public void setDeal(Deal deal) {
        this.deal = deal;
        deal.setOrder(this);
    }

    /**
     *   생성 메소드
     * - order 객체만 생성한다고 해서 되는 것이 아니라
     * - order 객체에 member, orderItem, deal 정보들을 넣어줘야함
     * - 이럴 때는 생성 메소드로 뽑아서 하는 것이 낫다 => 유지보수, 관리가 편함
     */
    public static Order createOrder(Member member, Deal deal, OrderItem... orderItems) {
        // OrderItem... 문법: 리스트로 넘겨야하므로
        Order order = new Order();

        order.setMember(member);
        order.setDeal(deal);
        for (OrderItem orderItem : orderItems)
            order.addOrderItem(orderItem);

        order.setStatus(OrderStatus.ORDER);
        order.setOrderDate(LocalDateTime.now());

        return order;
    }

    /**
     * 비지니스 로직
     */
    public void completeDeal() {    // 거래 완료
        this.deal.setStatus(DealStatus.COMP);   // 변경감지
    }

    public void cancel() {
        this.setStatus(OrderStatus.CANCEL); // 취소
        for (OrderItem orderItem : orderItems)  // 해당 주문의 상품 리스트들
            orderItem.cancel(); // 전부 취소
    }

    /**
     * 조회 로직
     */
    public int getTotalPrice() {    // 전체 주문가격 조회
        int totalPrice = 0;
        for (OrderItem orderItem : orderItems) // 해당 주문의 상품 리스트들의
            totalPrice += orderItem.getTotalPrice();    // 각각의 가격을 더하여 전체 합계

        return totalPrice;  // 합계를 리턴
    }

    // Setter
    private void setOrderDate(LocalDateTime now) {
        this.orderDate = now;
    }

    private void setStatus(OrderStatus status) {
        this.status = status;
    }
}


