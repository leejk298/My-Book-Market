package mybook.mymarket.service;

import mybook.mymarket.domain.Address;
import mybook.mymarket.domain.Member;
import mybook.mymarket.repository.MemberRepository;
import mybook.mymarket.service.dto.MemberDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;


@SpringBootTest     // 스프링 위에서 테스트
@RunWith(SpringRunner.class)    // 스프링 컨테이너 안에서 실행
@Transactional      // 데이터 변겅이 일어나므로, 롤백시키기 위해
public class MemberServiceTest {
    @Autowired  // 스프링이 스프링 빈에 있는 memberRepository 를 주입해줌
    MemberRepository memberRepository;
    @Autowired
    MemberService memberService;

    /**
     * validation 을 위한 최적의 장소는 Service 계층보다 Model 생성자 내부다.
     * Model 객체는 어떤 계층에서든 만들어질 수 있고, 모든 계층을 통과할 수 있는 존재다.
     * 따라서 애초에 Model 객체가 만들어질 때 검증 절차를 수행하면, 어떤 계층에서 생성하던지,
     * 어떤 계층으로부터 넘어오던지 신경쓰지 않고 해당 객체를 안전하게 사용할 수 있게 된다.
     * => memberService 에서는 회원가입, 회원수정 시 IllegalStateException("이미 존재하는 회원입니다.") 중복검사 테스트
     */

    /**
     * IllegalStateException
     * : 회원가입(join), 회원수정(updateMember) 시에 validateDuplicateMember(nickName) 메소드를 타고,
     *   해당 List 컬렉션이 비어있으면 중복 X, 비어있지않으면 중복 O 이므로 예외가 발생하게 된다
     */

    @Test
    public void 회원가입() throws Exception {
        // given
        Member member = new Member("joinMember", "1234", "이정규", new Address("a", "b", "c"));

        // when
        Long joinId = memberService.join(new MemberDto(member));

        // then
        assertThat(member.equals(memberRepository.findOne(joinId)));    // 같은 회원인지
    }

    @Test(expected = IllegalStateException.class)
    public void 중복검사() throws Exception {
        // given
        Member member1 = new Member("joinMember", "1234", "이정규", new Address("a", "b", "c"));
        Member member2 = new Member("joinMember", "12", "정규", new Address("a", "b", "c"));

        // when
        memberService.join(new MemberDto(member1));
        memberService.join(new MemberDto(member2));  // 예외 발생: 이미 존재하는 회원입니다.

        // then
        fail("id가 중복되므로 예외가 발생해야 한다.");  // 여기로 오면 잘못 작성한 테스트 케이스
    }

    @Test
    public void 회원_수정() throws Exception {
        // given
        Member member = new Member("joinMember", "1234", "이정규", new Address("a", "b", "c"));
        Long joinId = memberService.join(new MemberDto(member));

        // when
        Member findMember = memberRepository.findOne(joinId);
        findMember.changeMember("updateMember", "1234", "이정규", new Address("a", "b", "c"));

        // then
       assertThat(!member.equals(findMember));  // 수정이 값이 바뀌므로
    }

    @Test(expected = IllegalStateException.class)
    public void 회원_수정_중복검사() throws Exception {
        // given
        // member1 - joinMember1, member2 - joinMember2  회원가입
        Member member1 = new Member("joinMember1", "1234", "이정규", new Address("a", "b", "c"));
        Long joinId1 = memberService.join(new MemberDto(member1));
        Member member2 = new Member("joinMember2", "12", "정규", new Address("a", "b", "c"));
        memberService.join(new MemberDto(member2));

        // when
        // member1 - joinMember1 => member2 id로 수정
        Member updateMember = memberRepository.findOne(joinId1);
        updateMember.changeMember(member2.getNickName(), "1234", "이정규", new Address("a", "b", "c"));
        // => 예외 발생해야함
        // member1의 현재 id와 수정 id가 다를 때 => joinMember1 != joinMember2
        if (!(member1.getNickName().equals(updateMember.getNickName()))) {  // true
            List<Member> findMembers = memberRepository.findByName(updateMember.getNickName()); // 수정될 id로 검색

            if (!findMembers.isEmpty()) {  //   컬렉션이므로 isEmpty(), 해당 id가 이미 있으므로
                throw new IllegalStateException("이미 존재하는 회원입니다.");  // 예외처리
            }
        }

        // then
        fail("id가 중복되므로 예외가 발생해야 한다.");  // 여기로 오면 잘못 작성한 테스트 케이스
    }
}