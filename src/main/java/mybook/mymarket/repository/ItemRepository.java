package mybook.mymarket.repository;

import lombok.RequiredArgsConstructor;
import mybook.mymarket.domain.item.Item;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor    // final 키워드를 가진 필드(em)로 생성자 만들어줌
public class ItemRepository {
    // @Autowired  // 스프링데이터 JPA 가 지원해줌, 한 개일 때는 생략 가능
    private final EntityManager em; // @RequiredArgsConstructor => 생성자를 통해 엔티티매니저를 주입받게됨

    /**
     * 상품 저장을 따로 안해도 되는 이유
     => Cascade 옵션: Register 시 해당 상품도 저장되게끔
     */
//    public void save(Item item) {   // 상품 저장
//        if(item.getId() == null) {  // item 처음 저장할 때
//            em.persist(item);   // commit 시 DB에 반영됨 => Insert 쿼리
//        } else {    // 처음이 아닌 경우
//            em.merge(item); // update 와 비슷함
//        }
//    }

    public Item findOne(Long id) {  // 상품 조회 (단권 조회)
        return em.find(Item.class, id);
    }

    /**
     * 상품 등록 시 같은 회원이 같은 상품을 등록하면 수량만 업데이트하게끔
     */
    public Optional<Item> findByMemberAndItem(Long id, String name) { // 해당 id로 같은 상품을 등록했는지
        return em.createQuery("select i from Register r " +
                        "join r.item i join r.member m " +
                        "where m.id = :id and " +
                        "i.name like :name", Item.class)
                .setParameter("id", id)
                .setParameter("name", name)
                .getResultList().stream().findAny();
    }

    public Optional<Item> findByName(String name) {
        return em.createQuery("select i from Item i where i.name = :name", Item.class)
                .setParameter("name", name)
                .getResultList().stream().findAny();
    }

    public List<Item> findAll() {   // 상품 조회 (리스트 조회)
        return em.createQuery("select i from Item i", Item.class)  // (JPQL, ClassType)
                .getResultList();
    }
}
