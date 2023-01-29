package mybook.mymarket.repository.register.query;

import lombok.RequiredArgsConstructor;
import mybook.mymarket.repository.RegisterSearch;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

@Repository
@RequiredArgsConstructor
// 화면에 의존적이지만 로직은 쿼리로 조금 무겁고 복잡하고
// 하지만 이게 repository 에 있으면 용도가 굉장히 애매해지므로 따로 관리하자
// repository 용도는 엔티티 조회하는데 사용하므로
// => 핵심 엔티티를 찾거나 조회하려면 repository, 엔티티가 아닌 화면에 fit 한 쿼리들은 여기로
// 장점: 화면과 관련된 것들은 쿼리들과 밀접하기때문에 따로 디렉토리를 두어서 관리하면
// 관심사 분리도 가능하고 두 개가 서로 라이프사이클이 다르기때문에 유지보수도 편함
public class RegisterQueryRepository {
    private final EntityManager em;

    // RegisterDto 안쓰고 별도로 RegisterQueryDto 만든 이유
    // 1. Repository (RegisterQueryRepository) 가 Controller 에 있는 RegisterDto 를 참조를 하는 꼴이 됨
    // => 의존관계가 순환이 되게됨 (C -> R -> C)
    // 2. findRegisters() 이 만드는 것이므로 RegisterQueryDto 가 알아야하므로 같은 패키지로 관리
    public List<RegisterQueryDto> findAllByDto() {
        // ToOne 관계 (M, I) Dto 로 직접 조회 => SQL 직접 조인 (fetch join 아님) => fetch join 불가능
        List<RegisterQueryDto> registers = findRegisters();

        return registers;
    }

    public List<RegisterQueryDto> findRegisters() {
        return em.createQuery(
                        "select new mybook.mymarket.repository.register.query." +
                                "RegisterQueryDto(r.id, m.id, i.id, m.nickName, i.name, i.price, i.stockQuantity, r.registerDate, r.status)" +
                                "from Register r " +
                                "join r.member m " +
                                "join r.item i", RegisterQueryDto.class)
                .getResultList();
    }

    public List<RegisterQueryDto> findAllByDto_search(RegisterSearch registerSearch) {
        // ToOne 관계 (M, I) => SQL 직접 조인 (fetch join 아님)
        List<RegisterQueryDto> registers = findRegisters_search(registerSearch);

        return registers;
    }

    public List<RegisterQueryDto> findRegisters_search(RegisterSearch registerSearch) {
        // 등록 - (등록)상품, (등록)회원 => join
        String jpql = "select new mybook.mymarket.repository.register.query." +
                "RegisterQueryDto(r.id, m.id, i.id, m.nickName, i.name, i.price, i.stockQuantity, r.registerDate, r.status)" +
                "from Register r join r.member m join r.item i";
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
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }

            jpql += " i.name like :iName";
        }

        TypedQuery<RegisterQueryDto> query = em.createQuery(jpql, RegisterQueryDto.class)
                .setMaxResults(1000); //최대 1000건

        if (registerSearch.getRegisterStatus() != null)
            query = query.setParameter("status", registerSearch.getRegisterStatus());

        if (StringUtils.hasText(registerSearch.getNickName()))
            query = query.setParameter("name", registerSearch.getNickName());

        if (StringUtils.hasText(registerSearch.getItemName()))
            query = query.setParameter("iName", registerSearch.getItemName());

        return query.getResultList();
    }

    public List<RegisterQueryDto> findMyAllByDto(Long memberId) {
        List<RegisterQueryDto> myRegisters = findMyRegistres(memberId);

        return myRegisters;
    }

    public List<RegisterQueryDto> findMyRegistres(Long memberId) {
        return em.createQuery(
                "select new mybook.mymarket.repository.register.query." +
                        "RegisterQueryDto(r.id, m.id, i.id, m.nickName, i.name, i.price, i.stockQuantity, r.registerDate, r.status)" +
                        "from Register r join r.member m join r.item i " +
                        "where m.id = :memberId", RegisterQueryDto.class)
                .setParameter("memberId", memberId)
                .getResultList();
    }
}
