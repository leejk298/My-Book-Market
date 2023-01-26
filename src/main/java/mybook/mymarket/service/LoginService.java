package mybook.mymarket.service;

import lombok.RequiredArgsConstructor;
import mybook.mymarket.domain.Member;
import mybook.mymarket.exception.NotCorrespondingEmailException;
import mybook.mymarket.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service    // 스프링빈에 등록
@Transactional
@RequiredArgsConstructor    // final 키워드를 가진 필드(memberRepository)로 생성자를 만들어줌
public class LoginService {
    // Member 개체에 접근하기 위해서 MemberRepository 를 생성한 후 의존성 주입(DI)을 해준다
    private final MemberRepository memberRepository;     // final: 변경할 일 X, 컴파일 시점에 오류검사가능

    /**
     * 로그인
     */
    public Member login(String nickName, String password) {
        //Optional 클래스는 반환값이 Null 이 발생할 수도 있는 메서드에 사용하면 NPE 를 피할 수 있고,
        // 다양한 Optional 의 메서드를 통해 Null 이 발생했을 때 문제를 해결할 수 있다.
        Optional<Member> findMember = memberRepository.findByNickName(nickName);
        /**
         * 1. findByNickName() 메서드를 통해 반환받은 Optional 클래스가 적용된 Member 객체가
         *    Null 이 아닌지 확인하기 위해 orElseThrow() 메서드를 적용한다.
         * 2. 만약 Member 객체가 Null 이라면 사용자 정의 예외(NotCorrespondingEmailException)를 터뜨려준다.
         *    Member 객체가 Null 이 아니라면 checkPassword() 메서드를 통해 고객으로부터 전달받은
         *    비밀번호(password)와 객체의 비밀번호가 일치하는지 확인한다.
         *  2 - 1. 일치하지 않는다면 IllegalStateException() 예외를 터뜨려준다.
         *  2 - 2. 일치한다면 findByNickName() 메서드를 통해 반환받은 findMember 객체를 반환해준다.
         */
        if (!findMember.orElseThrow(() -> new NotCorrespondingEmailException("해당 ID는 존재하지 않습니다."))
                .checkPassword(password)) {
            throw new IllegalStateException("ID와 PW가 일치하지 않습니다.");
        }

        return findMember.get();
    }
}
