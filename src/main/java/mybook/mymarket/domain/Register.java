package mybook.mymarket.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mybook.mymarket.domain.item.Item;

import javax.persistence.*;
import java.time.LocalDateTime;

import static javax.persistence.FetchType.LAZY;

@Entity
@Getter
// protected Registration() { } => 다른 곳에서 생성자를 제약시키고, 생성 메소드 이용하라고 알리기 위해
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA 에서 protected 는 생성자 사용하지 말라는 뜻
public class Register {
    @Id @GeneratedValue
    @Column(name = "register_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id") // 외래키 매핑
    private Member member;  // 연관관계의 주인(Register.member) => 외래키 갖고있는 N 쪽

    @OneToOne(mappedBy = "register", cascade = CascadeType.ALL)
    private Item item; // 읽기전용, Item.register 에 의해 매핑됨
    // cascade: register 만 persist 해도 item 이 같이 따라옴, delete 도 마찬가지

    private LocalDateTime registerDate; // 등록 시간

    @Enumerated(EnumType.STRING)    // 무조건 STRING 으로, ORDINAL 은 숫자 => 필드 추가 시 오류발생
    private RegisterStatus status;  // 등록 상태 [REGISTER, CANCEL]

    /**
     * 연관관계 메소드 **
     * - 양방향 연관관계일 때 양쪽에 값을 다 넣어줘야함
     * - 한 쪽만 세팅하고 까먹을 수 있으므로 원자적으로 묶어서 처리
     * - 위치는 컨트롤하는 쪽에 있는 것이 낫다
     */
    public void setMember(Member member) {
        this.member = member;
        member.getRegisters().add(this);
    }

    public void addRegisterItem(Item item) {
        this.item = item;
        item.setRegister(this);
    }

    /**
     *   생성 메소드
     * - registration 객체만 생성한다고 해서 되는 것이 아니라
     * - registration 객체에 member, orderItem, deal 정보들을 넣어줘야함
     * - 이럴 때는 생성 메소드로 뽑아서 하는 것이 낫다 => 유지보수, 관리가 편함
     */
    public static Register createRegister(Member member, Item item) {
        Register register = new Register();

        register.setMember(member);
        register.addRegisterItem(item);

        register.setStatus(RegisterStatus.REGISTER);
        register.setRegisterDate(LocalDateTime.now());

        return register;
    }

    /**
     * 비지니스 로직
     */
    public void cancel() {  // 등록 취소
        this.setStatus(RegisterStatus.CANCEL);  // 취소
        item.cancel();
    }

    // Setter
    public void setRegisterDate(LocalDateTime now) {
        this.registerDate = now;
    }

    public void setStatus(RegisterStatus status) {
        this.status = status;
    }
}
