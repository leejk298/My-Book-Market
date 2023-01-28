package mybook.mymarket.api;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import mybook.mymarket.controller.form.LoginForm;
import mybook.mymarket.domain.Member;
import mybook.mymarket.service.LoginService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController // @RestController == @Controller + @ResponseBody
// @ResponseBody: data 자체를 바로 Json 이나 XML 로 바로 보내기위헤
@RequiredArgsConstructor    // final 키워드를 가진 필드(loginService)로 생성자를 만들어줌
public class LoginApiController {
    private final LoginService loginService;    // Controller 가 Service 갖다씀

    /**
     * 로그인
     */
    @PostMapping("/api/login")
    public LoginMemberResponse loginMember(@RequestBody @Valid LoginForm loginForm) {
        // @Valid: MemberForm 에 있는 @NotEmpty 를 읽어서 Validation 기능을 수행하게됨
        Member loginMember = loginService.login(loginForm.getNickName(), loginForm.getPassword());

        // 등록하여 반환된 필드 값들을 Json 형식으로 보여줌
        // @AllArgsConstructor: 모든 파라미터를 담는 생성자
        return new LoginMemberResponse(loginMember.getId(), loginMember.getNickName());
    }

    @Data
    @AllArgsConstructor
    static class LoginMemberResponse {  // 응답 값
        private Long id;    // 로그인하게 되면 id
        private String nickName;    // 닉네임 리턴
    }
}
