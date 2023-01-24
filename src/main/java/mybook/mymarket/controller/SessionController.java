package mybook.mymarket.controller;

import lombok.extern.slf4j.Slf4j;
import mybook.mymarket.controller.form.LoginForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller // 스프링 빈에 등록
@Slf4j  // Lombok 에 Slf4j => 로그
public class SessionController {
    /**
     * Logger log = LoggerFactory.getLogger(getClass());
     * : 로그를 뽑을 수 있음 => @Slf4j 와 같음
     */

    /**
     * Index
     */
    @GetMapping("/")    // '/' 접근요청이 오면
    public String homeLogin(@SessionAttribute(name = "memberId", required = false) Long memberId, Model model){
        /**
         * @SessionAttribute
         * : 컨트롤러 밖(인터셉터 또는 필터 등)에서 만들어 준 세션 데이터에 접근할 때 사용한다.
         * => 인터셉터에서 session 을 넣어주고 @SessionAttribute 어노테이션을 이용해서 Session 을 가져온다.
         * 자동 컨버전이 가능하기 때문에 굉장히 편리하다.
         * 또한 HttpSession 으로도 가져올 수 있으나, 타입 컨버전을 해야하기 때문에 번거롭다.
         * request.getSession()를 사용하면 기본 값이 true, 로그인 하지 않을 사용자도 의미 없는 세션이 만들어진다.
         * 따라서 세션을 찾아서 사용하는 시점에는 false 옵션을 사용하여 세션을 생성하지 않도록 설정한다.
         */

        /**
         * 세션 데이터(memberId)가 없으면 => 로그인 정보 X
         * :Login 페이지로 이동
         */
        if(memberId == null) {  // 세션 데이터(memberId)가 없으면 => 로그인 정보 X
            // Model: Controller 에서 View 로 넘어갈 때 데이터를 Model 에 실어서 넘김
            model.addAttribute("loginForm", new LoginForm());
            // 화면을 이동할 때 loginForm 이라는 빈 껍데기 객체를 가져감
            // 이유: 빈 화면이니까 아무것도 없을 수도 있지만, validation 등을 해줄 수 있기 때문에

            return "logins/loginForm";  // loginForm.html 로 넘어감
        }

        /**
         * 세션 데이터(memberId)가 있으면 => 로그인 O
         * => 메인 페이지(home.html)로 이동
         */
        return "home";
    }

}
