package mybook.mymarket.domain;


import lombok.Getter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
public class Member {
    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String nickName;    // 로그인 id, 중복 x

    private String password;    // pw

    private String userName;    // 이름

    @Embedded   // 내장되어있는
    private Address address;

    /**
     * 컬렉션은 필드에서 초기화하자
     * null 문제에서 안전하고 필드 레벨에서 생성하는 것이 안전함
     */
    @OneToMany(mappedBy = "member") // Register 테이블의 member 에 의해 매핑됨 => 읽기전용
    private List<Register> registers = new ArrayList<>();

    @OneToMany(mappedBy = "member") // Order 테이블의 member 에 의해 매핑됨 => 읽기전용
    private List<Order> orders = new ArrayList<>();

    /**
     * 로그인 검사
     */
    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    /**
     * 변경감지에서 update 값 세팅
     */
    public void changeMember(String nickName, String password, String userName, Address address) {
       this.nickName = nickName;
       this.password = password;
       this.userName = userName;
       this.address = address;
    }

    // Setter
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}
