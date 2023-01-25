package mybook.mymarket.repository;

import lombok.RequiredArgsConstructor;
import mybook.mymarket.controller.dto.MemberDto;
import mybook.mymarket.domain.Member;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

@Repository // 스프링빈에 등록
@RequiredArgsConstructor    // final 키워드의 필드(em)를 가지고 생성자를 만들어줌
public class MemberRepository {
    // @Autowired   // 스프링데이터 JPA 가 지원해줌, 한 개일 때는 생략가능
    private final EntityManager em; // 생성자를 통해 주입받게 됨
    /**
     @PersistenceContext // JPA 의 엔티티매니저를 스프링이 주입
     private EntityManager em;
     */

    public void save(Member member) {   // 회원 저장
        em.persist(member); // commit 시 DB에 반영됨 => Insert 쿼리
    }

    public Member findOne(Long id) {    // 회원 찾기
        return em.find(Member.class, id);   // (클래스 타입, 키)
    }

    public List<Member> findAll() {     // 회원 찾기(리스트 조회) => JPQL
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();   // from 의 대상은 엔티티 객체: m
    }

    // 회원가입, 회원 정보 수정 => 단순 중복 체크
    public List<Member> findByName(String nickName) {   // 닉네임으로 특정 회원 찾기
        return em.createQuery("select m from Member m where m.nickName = :nickName", Member.class)
                .setParameter("nickName", nickName)
                .getResultList();
    }

    // 로그인 => NullPointException 발생 가능 => Optional 클래스
    public Optional<Member> findByNickName(String nickName) {
        return em.createQuery("select m from Member m where m.nickName = :nickName", Member.class)
                .setParameter("nickName", nickName)
                .getResultList().stream().findAny();
    }
    // 데이터베이스에서 nickName 정보는 중복이 존재하지 않는 속성이기 때문에
    // getResultList() 대신 getSingleResult() 를 사용하려 했지만,
    // getSingleResult()는 데이터베이스에 해당 이메일 속성을 가진 객체가 존재하지 않으면
    // Null 을 반환하는 것이 아닌 예외가 발생하는 문제가 발생하기 때문에 getResultList()를 사용
}

