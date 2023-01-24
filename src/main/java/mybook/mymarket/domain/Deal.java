package mybook.mymarket.domain;


import lombok.Getter;

import javax.persistence.*;

import static javax.persistence.FetchType.LAZY;

@Entity
@Getter
public class Deal {
    @Id @GeneratedValue
    @Column(name = "deal_id")
    private Long id;

    @OneToOne(mappedBy = "deal", fetch = LAZY)
    private Order order;

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING)
    private DealType type;  // 거래형태 [DIRECT, DELIVERY] => 직거래, 배송

    @Enumerated(EnumType.STRING)
    private DealStatus status;   // 거래상태 [WAIT, COMP]


    // Setter
    public void setOrder(Order order) {
        this.order = order;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public void setStatus(DealStatus status) {
        this.status = status;
    }

    public void setType(DealType type) {
        this.type = type;
    }
}
