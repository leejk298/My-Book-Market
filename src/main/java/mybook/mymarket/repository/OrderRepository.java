package mybook.mymarket.repository;

import lombok.RequiredArgsConstructor;
import mybook.mymarket.domain.Order;
import mybook.mymarket.domain.OrderItem;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {
    private final EntityManager em;

    public void save(Order order) { // 상품 저장
        em.persist(order);
    }

    public Order findOne(Long id) { // 단권 조회
        return em.find(Order.class, id);
    }

    public Order findOrderMember(Long id) {
        return em.createQuery("select o from Order o join fetch o.member m " +
                "where o.id = :id", Order.class)
                .setParameter("id", id)
                .getSingleResult();

    }

    public Order findOrderDeal(Long id) {
        // 특정 주문과 관련된 거래 찾기 (fetch join, ToOne 관계)
        return em.createQuery(
                "select o from Order o join fetch o.deal d " +
                        "where o.id = :id", Order.class)
                .setParameter("id", id)
                .getSingleResult();
    }

    /**
     * 일반 join - v2
     */
    public List<Order> findAllWithMemberDeal() { // ToOne 관계 (M, D)
        // Order 를 조회하는데 Member 와 Delivery 를 from 절에서 join
        return em.createQuery(
                        "select o from Order o " +
                                "join o.member m " +
                                "join o.deal d", Order.class)
                .getResultList();
    }

    public List<Order> findAllWithMemberDealByString(OrderSearch orderSearch) {
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

    public List<Order> findMyOrders(Long memberId) {    // 나의 주문 정보
        return em.createQuery(
                        "select o from Order o " +
                                "join o.member m " +
                                "join o.deal d " +
                                "where m.id = :memberId", Order.class)
                .setParameter("memberId", memberId)
                .getResultList();
    }

    /**
     * Fetch Join - v3
     * 성능 최적화 => N + 1 문제 성능 문제의 90프로 해결
     */
    public List<Order> findAllWithMemberDeal_fetch() {
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

    public List<Order> findAllWithMemberDealByString_fetch(OrderSearch orderSearch) {
        String jpql = "select o from Order o join fetch o.member m join fetch o.deal d";
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

    public List<Order> findMyOrders_fetch(Long memberId) {
        return em.createQuery(
                        "select o from Order o " +
                                "join fetch o.member m " +
                                "join fetch o.deal d " +
                                "where m.id = :memberId", Order.class)
                .setParameter("memberId", memberId)
                .getResultList();
    }

    /**
     * (주문 취소), (주문 거래 완료 API)에서 사용
     * 특정 주문과 관련된 주문상품, 상품, 등록, 회원 정보를 가져옴
     * => OI - I (ToOne 관계), I - R (ToOne 관계), R - M (ToOne 관계)
     * => Fetch join: 엔티티 영속화
     */
    public List<OrderItem> findOrderItems_fetch(Long orderId) {
        List<OrderItem> orderItems = em.createQuery(
                        "select oi from OrderItem oi " +
                                "join fetch oi.item i " +
                                "join fetch i.register r " +
                                "join fetch r.member m " +
                                "where oi.order.id in :orderId", OrderItem.class)
                .setParameter("orderId", orderId)  // 파라미터 바인딩
                .getResultList();

        return orderItems;
    }
}
