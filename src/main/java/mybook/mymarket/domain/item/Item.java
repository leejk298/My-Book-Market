package mybook.mymarket.domain.item;


import lombok.Getter;
import mybook.mymarket.domain.Register;
import mybook.mymarket.domain.RegisterStatus;
import mybook.mymarket.exception.NotEnoughStockException;

import javax.persistence.*;

import static javax.persistence.FetchType.LAZY;

@Entity
@Getter
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)   // 한 테이블
@DiscriminatorColumn    // 구분
public abstract class Item {
    @Id @GeneratedValue
    @Column(name = "item_id")
    private Long id;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "register_id")   // 외래키 매핑
    private Register register;  // 연관관계의 주인(Item.register)

    private String name;

    private String author;

    private int price;

    private int stockQuantity;

    /**
     * 비지니스 로직
     * : 도메인 주도 설계에서 엔티티 자체에서 해결할 수 있는 것들은 엔티티 안에서
     *   비지니스 로직을 넣어 처리하는게 객체지향적이고 응집도가 높다.
     */
    public void addStock(int quantity) {    // 재고 수량 증가
        this.stockQuantity += quantity;     // 현재 수량 갱신
    }

    public void removeStock(int quantity) {
        int resStock = this.stockQuantity - quantity;

        if (resStock == 0) {    // 재고 소진 시 등록 취소
            this.register.setStatus(RegisterStatus.CANCEL);
        }

        if (resStock < 0) {  // 예외, 남은수량이 0 보다 작으면
            throw new NotEnoughStockException("need more stock");
        }
        // 예외가 발생하지 않으면
        this.stockQuantity = resStock;  // 현재수량을 남은수량으로 갱신
    }

    public void cancel() {  // 등록 취소 => 상품 재고 원복
        this.removeStock(stockQuantity);
    }

    /** 변경감지에서 update 값 세팅 */
    public void changeItem(String name, int price, int stockQuantity) {
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    // Setter
    public void setRegister(Register register) {
        this.register = register;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}
