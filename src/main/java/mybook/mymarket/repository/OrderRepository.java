package mybook.mymarket.repository;

import lombok.RequiredArgsConstructor;
import mybook.mymarket.domain.Order;
import mybook.mymarket.repository.order.query.OrderItemQueryDto;
import mybook.mymarket.repository.order.query.OrderQueryDto;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public List<Order> findMyOrders(Long memberId) {    // 나의 주문 정보
        return em.createQuery(
                        "select o from Order o " +
                                "join o.member m " +
                                "join o.deal d " +
                                "where m.id = :memberId", Order.class)
                .setParameter("memberId", memberId)
                .getResultList();
    }

    public List<Order> findAllWithMemberDeal() { // ToOne 관계 (M, D)
        // Order 를 조회하는데 Member 와 Delivery 를 from 절에서 join
        return em.createQuery(
                        "select o from Order o " +
                                "join o.member m " +
                                "join o.deal d", Order.class)
                .getResultList();
    }

    public List<OrderQueryDto> findAllByString_optimization(OrderSearch orderSearch) {
        List<Order> orders = findAllByString_fetch(orderSearch);

        List<OrderQueryDto> result = orders.stream().map(o -> new OrderQueryDto(o.getId(), o.getMember().getId(), o.getMember().getNickName(), o.getOrderDate(),
                o.getStatus(), o.getDeal().getStatus(), o.getDeal().getType(), o.getDeal().getAddress())).collect(Collectors.toList());

        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(toOrderIds(result));

        result.forEach(orderQueryDto -> orderQueryDto.setOrderItems(orderItemMap.get(orderQueryDto.getOrderId())));

        return result;
    }

    public List<OrderQueryDto> findMyOrders_optimization(Long memberId) {
        List<Order> myOrders = findMyOrders_fetch(memberId);

        List<OrderQueryDto> result = myOrders.stream().map(o -> new OrderQueryDto(o.getId(), o.getMember().getId(), o.getMember().getNickName(), o.getOrderDate(),
                o.getStatus(), o.getDeal().getStatus(), o.getDeal().getType(), o.getDeal().getAddress())).collect(Collectors.toList());

        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(toOrderIds(result));

        result.forEach(orderQueryDto -> orderQueryDto.setOrderItems(orderItemMap.get(orderQueryDto.getOrderId())));

        return result;
    }

    private static List<Long> toOrderIds(List<OrderQueryDto> result) {
        List<Long> orderIds = result.stream()
                .map(o -> o.getOrderId())
                .collect(Collectors.toList());

        return orderIds;
    }

    private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long> orderIds) {
        List<OrderItemQueryDto> orderItems = em.createQuery(
                        "select new mybook.mymarket.repository.order.query." +
                                "OrderItemQueryDto(oi.order.id, r.member.id, i.id, i.name, r.member.nickName, oi.count, oi.orderPrice) " +
                                "from OrderItem oi " +
                                "join oi.item i " +
                                "join i.register r " +
//                                "join r.member m " +
                                "where oi.order.id in :orderIds", OrderItemQueryDto.class)
                .setParameter("orderIds", orderIds)
                .getResultList();

        Map<Long, List<OrderItemQueryDto>> orderItemMap = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItemQueryDto::getOrderId));

        return orderItemMap;
    }

    /**
     * Fetch Join
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

    public List<Order> findMyOrders_fetch(Long memberId) {
        return em.createQuery(
                        "select o from Order o " +
                                "join fetch o.member m " +
                                "join fetch o.deal d " +
                                "where m.id = :memberId", Order.class)
                .setParameter("memberId", memberId)
                .getResultList();
    }

    public List<Order> findAllByString_fetch(OrderSearch orderSearch) {
        /** 파라미터가 null 이거나 필요하지 않은 경우 => 동적쿼리 필요
         * JPQL 쿼리를 문자로 생성하기는 번거롭고, 실수로 인한 버그가 충분히 발생할 수 있다.*/

        String jpql = "select o from Order o " +
                "join fetch o.member m " +
                "join fetch o.deal d";
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
}
