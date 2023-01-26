package mybook.mymarket.service;


import mybook.mymarket.controller.dto.MemberDto;
import mybook.mymarket.controller.form.LoginForm;
import mybook.mymarket.domain.Address;
import mybook.mymarket.domain.Member;
import mybook.mymarket.exception.NotCorrespondingEmailException;
import mybook.mymarket.repository.MemberRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;


import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.fail;

@SpringBootTest     // 스프링 위에서 테스트
@RunWith(SpringRunner.class)    // 스프링 컨테이너 안에서 실행
@Transactional  // 데이터 변경이 일어나므로, 롤백시키기 위해
public class LoginServiceTest {
    @Autowired  // 스프링이 스프링 빈에 있는 memberRepository 를 주입해줌
    LoginService loginService;
    @Autowired
    MemberService memberService;

    /**
     * validation 을 위한 최적의 장소는 Service 계층보다 Model 생성자 내부다. 
     * Model 객체는 어떤 계층에서든 만들어질 수 있고, 모든 계층을 통과할 수 있는 존재다. 
     * 따라서 애초에 Model 객체가 만들어질 때 검증 절차를 수행하면, 어떤 계층에서 생성하던지,
     * 어떤 계층으로부터 넘어오던지 신경쓰지 않고 해당 객체를 안전하게 사용할 수 있게 된다.
     * => loginService 에서는 NotCorrespondingEmailException("해당 ID는 존재하지 않습니다."),
     * IllegalStateException("ID와 PW가 일치하지 않습니다.") 테스트
     */

    /**
     * NotCorrespondingEmailException
     * : memberRepository.findByNickName(nickName)로 반환되는 Optional 객체가 null 인 경우에 발생함
     * IllegalStateException
     * : 위 Optional 객체가 null 이 아니면 checkPassword() 메소드를 타고, 일치하지 않는 경우에 발생함
     */
    @Test(expected = NotCorrespondingEmailException.class)
    public void 로그인_id_Pw_X() throws Exception {    // 1. id, pw 둘 다 null
        // given: ~~이 주어졌을 때 (= 객체 생성 및 초기화)
        LoginForm form = new LoginForm();
        form.setNickName("");   // id, pw 둘 다 null => 오류
        form.setPassword("");

        // when: ~~하면 (검증해야할 대상)
        loginService.login(form.getNickName(), form.getPassword()); // 해당 ID는 존재하지 않습니다.

        // then: 검증
        fail("id, pw 둘 다 없으므로 예외가 발생해야 한다.");    // 여기로 오면 잘못 작성한 테스트 케이스
    }

    private Member createAndJoinMember() {
        Member member = new Member("loginTest", "1234", "이정규", new Address("a", "b", "c"));
        memberService.join(new MemberDto(member));
        return member;
    }

    @Test(expected = NotCorrespondingEmailException.class)
    public void 로그인_id_X_Or_Pw() throws Exception {    // 2. id가 null, pw는 존재
        // given
        // 회원 가입: id - loginTest, pw - 1234
        Member member = createAndJoinMember();
        // 가입된 id로 로그인하되, id x
        LoginForm form = new LoginForm();
        form.setNickName("");   // id null => 오류
        form.setPassword(member.getPassword());

        // when
        loginService.login(form.getNickName(), form.getPassword()); // 해당 ID는 존재하지 않습니다.

        // then
        fail("id가 없으므로 예외가 발생해야 한다.");    // 여기로 오면 잘못 작성한 테스트 케이스
    }

    @Test(expected = IllegalStateException.class)
    public void 로그인_id_O_And_pw_X() throws Exception { // 3. id는 존재, pw가 null
        // given
        // 회원 가입: id - loginTest, pw - 1234
        Member member = createAndJoinMember();
        // 가입된 id로 로그인하되, pw x
        LoginForm form = new LoginForm();
        form.setNickName(member.getNickName());
        form.setPassword(""); // pw가 null

        // when
        loginService.login(form.getNickName(), form.getPassword()); // ID와 PW가 일치하지 않습니다.

        // then
        fail("id가 불일치하므로 예외가 발생해야 한다.");    // 여기로 오면 잘못 작성한 테스트 케이스
    }

    @Test(expected = NotCorrespondingEmailException.class)
    public void 로그인_id_불일치() throws Exception {  // 4. id 불일치
        // given
        // 회원 가입: id - loginTest, pw - 1234
        Member member = createAndJoinMember();
        // 가입된 id로 로그인하되, id 틀리게
        LoginForm form = new LoginForm();
        form.setNickName("login"); // id 불일치
        form.setPassword(member.getPassword());

        // when
        loginService.login(form.getNickName(), form.getPassword()); // 해당 ID는 존재하지 않습니다.

        // then
        fail("id가 불일치하므로 예외가 발생해야 한다.");    // 여기로 오면 잘못 작성한 테스트 케이스
    }

    @Test(expected = IllegalStateException.class)
    public void 로그인_pw_불일치() throws Exception {  // 5. pw 불일치
        // given
        // 회원 가입: id - loginTest, pw - 1234
        Member member = createAndJoinMember();
        // 가입된 id로 로그인하되, pw 틀리게
        LoginForm form = new LoginForm();
        form.setNickName(member.getNickName());
        form.setPassword("123");    // pw 불일치

        // when
        loginService.login(form.getNickName(), form.getPassword()); // 해당 ID는 존재하지 않습니다.

        // then
        fail("id가 불일치하므로 예외가 발생해야 한다.");    // 여기로 오면 잘못 작성한 테스트 케이스
    }

    @Test
    public void 로그인_성공() throws Exception {  // 6. 성공
        // given
        // 회원 가입: id - loginTest, pw - 1234
        Member member = createAndJoinMember();
        // 가입된 id로 로그인
        LoginForm form = new LoginForm();
        form.setNickName(member.getNickName());
        form.setPassword(member.getPassword());

        // when
        Member loginMember = loginService.login(form.getNickName(), form.getPassword());

        // then
        assertThat(loginMember.equals(member));  // 동등성 비교: 같은 값을 가져야 로그인이 가능하므로
    }

    /**
     * 동등성은 동등하다는 뜻으로 두 개의 객체가 같은 정보(값)를 갖고 있는 경우
     * 동일성은 동일하다는 뜻으로 두 개의 객체가 완전히 같은 (주소)경우
     */
}