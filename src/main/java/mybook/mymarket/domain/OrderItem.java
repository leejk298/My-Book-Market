package mybook.mymarket.domain;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mybook.mymarket.domain.item.Item;

import javax.persistence.*;

import static javax.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {
    @Id @GeneratedValue
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "item_id")   // 외래키 매핑
    private Item item;  // 연관관계의 주인(OrderItem.item) => 외래키를 갖고있는 N 쪽

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "order_id")  // 외래키 매핑
    private Order order;    // 연관관계의 주인(OrderItem.order) => 외래키를 갖고있는 N 쪽

    private int orderPrice;

    private int count;

    /** protected OrderItem() { // 다른 곳에서 생성자를 제약시키고, 생성 메소드 이용하라고 알리기 위해
     // JPA 는 protected 까지 기본 생성자를 만들 수 있도록 스펙상 허용
     // JPA 에서 protected 는 생성자 사용하지 말라는 뜻
     }
     ==> @NoArgsConstructor(access = AccessLevel.PROTECTED) 와 똑같은 코드 */

    /**
     * 생성 메소드
     */
    public static OrderItem createOrderItem(Item item, int orderPrice, int count) {
        OrderItem orderItem = new OrderItem();  // 객체 생성

        orderItem.setItem(item);    // 세팅
        orderItem.setOrderPrice(orderPrice);
        orderItem.setCount(count);

        item.removeStock(count);    // 해당 상품의 주문 개수만큼 재고 없애기

        return orderItem;   // 객체 리턴
    }


    /**
     * 비지니스 로직
     */
    public void cancel() { // 상품 취소
        getItem().addStock(count); // 재고수량 원상복구
        if (this.item.getStockQuantity() > 0) { // 주문 취소 -> 상품 취소 -> 등록 상품재고 증가 -> 등록 상태 변경감지
            this.item.getRegister().setStatus(RegisterStatus.REGISTER);
        }
    }

    /**
     * 조회 로직
     */
    public int getTotalPrice() {    // 전체 가격 = 개수 * 가격
        return getOrderPrice() * getCount();
    }

    // Setter
    public void setOrder(Order order) {
        this.order = order;
    }
    private void setCount(int count) {
        this.count = count;
    }

    private void setOrderPrice(int orderPrice) {
        this.orderPrice = orderPrice;
    }

    private void setItem(Item item) {
        this.item = item;
    }
}
