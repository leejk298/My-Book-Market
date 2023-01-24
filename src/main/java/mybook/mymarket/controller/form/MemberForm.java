package mybook.mymarket.controller.form;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
public class MemberForm {
    private Long id;

    @NotEmpty(message = "id는 필수입니다.")
    private String nickName;    // 로그인 id, 중복 x

    @NotEmpty(message = "pw는 필수입니다.")
    private String password;    // pw

    @NotEmpty(message = "이름은 필수입니다.")
    private String userName;    // 이름

    private String city;
    private String street;
    private String zipcode;
}
