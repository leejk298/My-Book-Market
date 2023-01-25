package mybook.mymarket.repository.register.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
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
        // ToOne 관계 (M, I) => SQL 직접 조인 (fetch join 아님)
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
