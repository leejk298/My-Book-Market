package mybook.mymarket.service;

import lombok.RequiredArgsConstructor;
import mybook.mymarket.domain.Member;
import mybook.mymarket.repository.MemberRepository;
import mybook.mymarket.service.dto.MemberDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service    // 스프링빈에 등록
@Transactional(readOnly = true) // 읽기전용: 리소스 낭비 X, default: false
@RequiredArgsConstructor    // final 키워드를 가진 필드(memberRepository)로 생성자를 만들어줌
public class MemberService {
    // @Autowired // 하나이므로 생략가능
    private final MemberRepository memberRepository;    // final: 변경할 일 X, 컴파일 시점에 오류검사가능

    /**
     * 생성자 Injection
     - 한 번 생성하면 중간에 바꿀 수 없으므로 좋음
     - 테스트케이스 작성 시 객체 생성할 때 주입받게끔 명확히 알 수 있음
     @Autowired  // 스프링이 스프링 빈에 있는 memberRepository 를 주입해줌
     public MemberService(MemberRepository memberRepository) {
     this.memberRepository = memberRepository;
     }
     =>  1. 최신버전 스프링에서는 생성자가 하나인 경우 @Autowired 없어 스프링이 자동으로 주입시켜줌
     +
     2. lombok 적용 => @RequiredArgsConstructor : final 키워드를 가진 필드로 생성자를 만들어줌
     */

    /**
     * 회원가입 => 데이터 변경 필요 => @Transactional
     */
    @Transactional // JPA 에서 모든 데이터 변경이나 로직들은 트랜잭션 안에서 실행되어야함
    public Long join(MemberDto memberDto) { // MemberDto - 서비스 계층 Dto
        validateDuplicateMember(memberDto.getNickName());    // 중복 회원 검증

        // Dto -> 엔티티
        Member member = new Member(memberDto.getNickName(), memberDto.getPassword(), memberDto.getUserName(), memberDto.getAddress());
        memberRepository.save(member);

        return member.getId();
    }

    private void validateDuplicateMember(String nickName) {   // 중복 검사
        List<Member> findMembers = memberRepository.findByName(nickName);// 같은 이름이 있는지 체크
        // Exception
        if(!findMembers.isEmpty()) {    // 컬렉션이므로 isEmpty()
            throw new IllegalStateException("이미 존재하는 회원입니다.");  // 예외처리
        }
    }

    /**
     * 회원 전체 조회
     */
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    /**
     * 회원 단권 조회
     */
    public Member findOne(Long id) {
        return memberRepository.findOne(id);
    }

    /**
     * 회원 수정
     * 변경 감지 (dirty checking)
     */
    @Transactional  // 데이터 변경하므로
    public void updateMember(Long id, MemberDto memberDto) {
        // 영속성 컨텍스트에 담기게 됨
        Member findMember = memberRepository.findOne(id);   // 영속 상태 엔티티

        /**
         * 회원 수정 시에도 중복검사 필요
         */
        // id를 바꿨을 때 && 해당 id가 이미 있을 때
        if (!(findMember.getNickName().equals(memberDto.getNickName()))) {   // 현재 id와 수정 id가 다를 때
            // 중복 회원 검증
            List<Member> findMembers = memberRepository.findByName(memberDto.getNickName());// 같은 이름이 있는지 체크
            // Exception
            if(!findMembers.isEmpty()) {    // 컬렉션이므로 isEmpty()
                throw new IllegalStateException("이미 존재하는 회원입니다.");  // 예외처리
            }
        }

        // 현재 id와 수정 id가 같으면 그대로 변경
        // => 역참조 방지하기 위해 파라미터로 넘김
        findMember.changeMember(memberDto.getNickName(), memberDto.getPassword(), memberDto.getUserName(), memberDto.getAddress());
    }

    /**  커맨드와 쿼리를 분리하자 - 유지보수 편함
     *   update(커맨드) 를 하면서 Member 를 쿼리하는 꼴이 됨
     *   커맨드: 엔티티를 싹 바꾸거나 하는 변경성 메소드
     *   변경성 메소드인데 Member 타입으로 반환하게 되면 조회하는 꼴이 됨
     =>  커맨드랑 쿼리가 같이있는 꼴 */
}
