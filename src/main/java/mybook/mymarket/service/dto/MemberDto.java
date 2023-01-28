package mybook.mymarket.service.dto;

import lombok.Getter;
import mybook.mymarket.domain.Address;
import mybook.mymarket.domain.Member;

import javax.validation.constraints.NotEmpty;

@Getter
public class MemberDto {
    private Long id;
    @NotEmpty
    private String nickName;
    @NotEmpty
    private String password;
    @NotEmpty
    private String userName;
    private Address address;

    /**
     * DTO 는 로직을 갖고있지 않는 순수한 데이터 객체이며 일반적으로 getter/setter 메소드만을 가진다.
     * 하지만 DTO 는 단순히 데이트를 옮기는 용도이기때문에 굳이 setter 를 이용해 값을 수정할 필요가 없고,
     * 생성자만을 사용하여 값을 할당하는 게 좋다.
     */
    protected  MemberDto() {
    }

    public MemberDto(Long id, String nickName) {    // 이름과 id
        this.id = id;
        this.nickName = nickName;
    }

    public MemberDto(Member member) {   // 엔티티 -> DTO
        this.id = member.getId();
        this.password = member.getPassword();
        this.nickName = member.getNickName();
        this.userName = member.getUserName();
        this.address = member.getAddress();
    }

    public MemberDto(String nickName, String password, String userName, String city, String street, String zipcode) {
        // => 역참조 방지하기 위해 파라미터로 넘김
        this.nickName = nickName;
        this.password = password;
        this.userName = userName;
        this.address = new Address(city, street, zipcode);
    }
}
