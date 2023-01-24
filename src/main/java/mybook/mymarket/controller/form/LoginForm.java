package mybook.mymarket.controller.form;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
public class LoginForm {
    @NotEmpty(message = "id는 필수입니다.")
    private String nickName;    // 로그인 id, 중복 x

    @NotEmpty(message = "pw는 필수입니다.")
    private String password;    // pw

}
