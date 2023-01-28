package mybook.mymarket.repository;


import lombok.RequiredArgsConstructor;
import mybook.mymarket.domain.Register;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

@Repository // 스프링빈에 등록
@RequiredArgsConstructor    // final 키워드의 필드(em)를 가지고 생성자 만들어줌
public class RegisterRepository {
    // @Autowired  // 스프링데이터 JPA 가 지원해줌, 한 개일 때는 생략 가능
    private final EntityManager em;
    // @RequiredArgsConstructor => 생성자를 통해 엔티티매니저를 주입받게됨

    public void save(Register register) {   // 등록 저장 -> 상품 저장, Cascade
        em.persist(register);
    }

    public Register findOne(Long id) {      // 등록 찾기
        return em.find(Register.class, id);
    }

    public List<Register> findAllByRegister() { // 모든 아이템과 주문한 회원을 가져오기 위해
        return em.createQuery("select r from Register r " +
                        "join r.item i join r.member m", Register.class)
                .getResultList();
    }

    public List<Register> findAllByString(RegisterSearch registerSearch) {  // where 절 조건에 맞는 등록 정보 가져오기
        // 등록 - (등록)상품, (등록)회원 => join
        String jpql = "select r from Register r join r.member m join r.item i";
        boolean isFirstCondition = true;

        //주문 상태 검색
        if (registerSearch.getRegisterStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }

            jpql += " r.status = :status";
        }

        //회원 이름 검색
        if (StringUtils.hasText(registerSearch.getNickName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }

            jpql += " m.nickName like :name";
        }

        // 상품명 검색
        if (StringUtils.hasText(registerSearch.getItemName())) {
            if(isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }

            jpql += " i.name like :iName";
        }

        TypedQuery<Register> query = em.createQuery(jpql, Register.class)
                .setMaxResults(1000); //최대 1000건

        if (registerSearch.getRegisterStatus() != null)
            query = query.setParameter("status", registerSearch.getRegisterStatus());

        if (StringUtils.hasText(registerSearch.getNickName()))
            query = query.setParameter("name", registerSearch.getNickName());

        if (StringUtils.hasText(registerSearch.getItemName()))
            query = query.setParameter("iName", registerSearch.getItemName());

        return query.getResultList();
    }

    public Register findOneByItem(Long id) { // 등록상품으로 해당 등록 가져오기
        return em.createQuery("select r from Register r join r.item i " +
                "where i.id = :id", Register.class)
                .setParameter("id", id)
                .getSingleResult();
    }

    public List<Register> findMyRegisters(Long memberId) {  // 회원으로 해당 등록 가져오기
        return em.createQuery(
                        "select r from Register r " +
                                "join r.member m " +
                                "join r.item i " +
                                "where m.id = :memberId", Register.class)
                .setParameter("memberId", memberId)
                .getResultList();
    }

    /**
     * Fetch Join
     * 성능 최적화 => N + 1 문제 성능 문제의 90프로 해결
     */
    public List<Register> findAllWithMemberItem() {
        // Register 를 조회하는데 Member 와 Item 을 from 절에서 join 하여
        // Select 절에서 같이 한 번에 가져오게 됨
        // => 한방 쿼리로 R, M, I 조인하여 Select 절에 넣어서 가져오는 것
        // M, I Lazy 로딩이여도 무시하고 Proxy 가 아닌 진짜 객체의 값을 한 번에 다 채워서 가져옴
        return em.createQuery(
                "select r from Register r " +
                        "join fetch r.member m " +
                        "join fetch r.item i", Register.class)
                .getResultList();
        // SQL 에는 fetch 라는 말이 없음 => JPA 에서 나온 것
    }

    public List<Register> findMyRegisters_fetch(Long memberId) {
        return em.createQuery(
                        "select r from Register r " +
                                "join fetch r.member m " +
                                "join fetch r.item i " +
                                "where m.id = :memberId", Register.class)
                .setParameter("memberId", memberId)
                .getResultList();
    }
}
