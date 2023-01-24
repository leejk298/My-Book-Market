package mybook.mymarket.controller;

import lombok.RequiredArgsConstructor;
import mybook.mymarket.controller.form.LoginForm;
import mybook.mymarket.domain.Member;
import mybook.mymarket.service.LoginService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

@Controller     // 스프링빈에 등록
@RequiredArgsConstructor    // final 키워드를 가진 필드(loginService, memberService)로 생성자를 만들어줌
public class LoginController {  // Controller 가 Service 갖다씀
    private final LoginService loginService;    // final: 변경할 일 X, 컴파일 시점에 오류검사가능

    /**
     * 로그인
     */
    @GetMapping("/login")   //  /login GetMapping: 열기, 접근
    public String loginForm(Model model) {
        model.addAttribute("loginForm", new LoginForm());

        return "/logins/loginForm";
    }

    @PostMapping("/login")  // /login PostMapping: 데이터 등록이 목표
    public String login(@Valid LoginForm form, BindingResult result, HttpServletRequest request) {
        // LoginForm 대신 Member 를 쓰면 안되는 이유: 화면에 쓰는 form 데이터와 엔티티가
        // 정확하게 일치하지도 않고 가져다가 쓰면 엔티티가 화면 종속적으로 되어 지저분해지므로
        // 유지보수하기도 힘들어지므로 엔티티와 폼을 분리해서 구현하자 (엔티티는 독립적이여야함)
        // @Valid: MemberForm 에 있는 @NotEmpty 를 읽어서 Validation 기능을 수행하게됨
        // BindingResult: @Valid 다음에 BindingResult 가 있으면 오류가 담겨서 실행하게됨
        // LoginForm 에 email 혹은 password 의 값이 존재하지 않을 때
        if (result.hasErrors())    // 담겨진 에러가 있으면
            return "/logins/loginForm"; // 화면까지 에러를 가져가서 뿌리게 됨

        Member loginMember = loginService.login(form.getNickName(), form.getPassword());  // 로그인 로직
        String memberId = String.valueOf(loginMember.getId());  // Long -> String 형변환

        /**
         * 로그인 후 세션에 저장
         **/
        // 세션이 있으면 원래의 세션 반환, 없으면 신규 세션을 생성
        HttpSession session = request.getSession();
        // 세션에 로그인 정보 보관
        session.setAttribute("memberId", memberId); // 이름이 memberId 이고 값이 memberId
//        session.setMaxInactiveInterval(60);

        return "home";
    }

    /**
     * 로그아웃
     */
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        // getSession(false) 를 사용해야 함 (세션이 없더라도 새로 생성하면 안되기 때문)
        HttpSession session = request.getSession(false);
//        세션이 있으면 기존 세션을 반환한다,
//        세션이 없으면 새로운 세션을 생성하지 않고, null 을 반환
        if (session != null)  // 세션이 있으면
            session.invalidate();   // 강제로 세션을 삭제한다.

        return "redirect:/";
    }

}
