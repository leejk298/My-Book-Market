package mybook.mymarket.repository;

import lombok.RequiredArgsConstructor;
import mybook.mymarket.domain.Member;
import mybook.mymarket.domain.Order;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;

    public void save(Order order) {
        em.persist(order);
    }

    public Order findOne(Long id) {
        return em.find(Order.class, id);
    }

    public List<Order> findAllByString(OrderSearch orderSearch) {
//              JPQL: 테이블이 아닌 객체이므로 o, m으로 표기
//              return em.createQuery("select o from Order o join o.member m" +
//                        " where o.status = :status " +
//                        " and m.name Like :name", Order.class)
//                        // 파라미터 바인딩
//                        .setParameter("status", orderSearch.getOrderStatus())
//                        .setParameter("name", orderSearch.getMemberName())
//
//                        // 페이징 => 100개부터 시작해서 몇 개 가지고 올것인지
//                        //.setFirstResult(100)
//
//                        // 최대 몇 개 가지고 올건 지
//                        .setMaxResults(1000)    // 최대 1000건
//                        // 결과값을 리스트로
//                        .getResultList();

        /** 파라미터가 null 이거나 필요하지 않은 경우 => 동적쿼리 필요
         * JPQL 쿼리를 문자로 생성하기는 번거롭고, 실수로 인한 버그가 충분히 발생할 수 있다.*/

        String jpql = "select o from Order o join o.member m join o.deal d";
        boolean isFirstCondition = true;

        // 주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }

            jpql += " o.status = :status";
        }

        // 거래 상태 검색
        if (orderSearch.getDealStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }

            jpql += " d.status = :dStatus";
        }

        // 회원 이름 검색
        if (StringUtils.hasText(orderSearch.getNickName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }

            jpql += " m.nickName like :name";
        }

        TypedQuery<Order> query = em.createQuery(jpql, Order.class)
                .setMaxResults(1000); //최대 1000건

        if (orderSearch.getOrderStatus() != null)
            query = query.setParameter("status", orderSearch.getOrderStatus());

        if (orderSearch.getDealStatus() != null)
            query = query.setParameter("dStatus", orderSearch.getDealStatus());

        if (StringUtils.hasText(orderSearch.getNickName()))
            query = query.setParameter("name", orderSearch.getNickName());

        return query.getResultList();
    }

    /**
     * Fetch Join
     * 성능 최적화 => N + 1 문제 성능 문제의 90프로 해결
     */
    public List<Order> findAllWithMemberDeal() {
        // Order 를 조회하는데 Member 와 Delivery 를 from 절에서 join 하여
        // Select 절에서 같이 한 번에 가져오게 됨
        // => 한방 쿼리로 O, M, D 조인하여 Select 절에 넣어서 가져오는 것
        // M, D Lazy 로딩이여도 무시하고 Proxy 가 아닌 진짜 객체의 값을 한 번에 다 채워서 가져오게 함
        return em.createQuery(
                        "select o from Order o " +
                                "join fetch o.member m " +
                                "join fetch o.deal d", Order.class)
                .getResultList();
        // SQL 에는 fetch 라는 말이 없음 => JPA 에서 나온 것
    }

    public List<Order> findMyOrders(Long memberId) {
        return em.createQuery(
                "select o from Order o " +
                        "join o.member m " +
                        "join o.deal d " +
                        "where m.id = :memberId", Order.class)
                .setParameter("memberId", memberId)
                .getResultList();
    }
}
