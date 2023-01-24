package mybook.mymarket.controller.dto;

import lombok.Getter;
import mybook.mymarket.controller.form.MemberForm;
import mybook.mymarket.domain.Address;
import mybook.mymarket.domain.Member;

@Getter
public class MemberDto {
    private Long id;
    private String nickName;
    private String password;
    private String userName;
    private Address address;

    /**
     * DTO 는 로직을 갖고있지 않는 순수한 데이터 객체이며 일반적으로 getter/setter 메소드만을 가진다.
     * 하지만 DTO 는 단순히 데이트를 옮기는 용도이기때문에 굳이 setter 를 이용해 값을 수정할 필요가 없고,
     * 생성자만을 사용하여 값을 할당하는 게 좋다.
     */
    protected  MemberDto() {
    }

    public MemberDto(String nickName) {   // 단순 이름
        this.nickName = nickName;
    }

    public MemberDto(Long id, String nickName) {    // 이름과 id
        this.id = id;
        this.nickName = nickName;
    }

    public MemberDto(Member member) {   // 엔티티 -> DTO
        this.id = member.getId();
        this.nickName = member.getNickName();
        this.password = member.getPassword();
        this.userName = member.getUserName();
        this.address = member.getAddress();
    }

    public MemberDto(MemberForm form) { // Form -> DTO
        this.nickName = form.getNickName();
        this.password = form.getPassword();
        this.userName = form.getUserName();
        Address address1 = new Address(form.getCity(), form.getStreet(), form.getZipcode());
        this.address = address1;
    }
}
