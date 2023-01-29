package mybook.mymarket.repository.order.query;


import lombok.RequiredArgsConstructor;
import mybook.mymarket.domain.Order;
import mybook.mymarket.domain.OrderItem;
import mybook.mymarket.repository.OrderSearch;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
// 화면에 의존적이지만 로직은 쿼리로 조금 무겁고 복잡하고
// 하지만 이게 repository 에 있으면 용도가 굉장히 애매해지므로 따로 관리하자
// repository 용도는 엔티티 조회하는데 사용하므로
// => 핵심 엔티티를 찾거나 조회하려면 repository, 엔티티가 아닌 화면에 fit 한 쿼리들은 여기로
// 장점: 화면과 관련된 것들은 쿼리들과 밀접하기때문에 따로 디렉토리를 두어서 관리하면
// 관심사 분리도 가능하고 두 개가 서로 라이프사이클이 다르기때문에 유지보수도 편함
public class OrderQueryRepository {
    private final EntityManager em;

    // OrderDto 안쓰고 별도로 findOrderQueryDtos 만든 이유
    // 1. Repository (OrderQueryRepository) 가 Controller 에 있는 OrderDto 를 참조를 하는 꼴이 됨
    // => 의존관계가 순환이 되게됨 (C -> R -> C)
    // OrderQueryDto 가 알아야하므로 같은 패키지로 관리
    // findOrderItems 용 쿼리를 또 따로 짜야함 => 일대다에서 다 부분 해결
    // ToMany 관계 (OI) => SQL 직접 조인
    // => join oi.item i: OrderItem 기준으로 Item 은 ToOne 관계이므로
    // 조인해도 데이터 row 수가 증가하지않으므로 조인하여서 최적화함
    // => OrderItem 과 Item 을 한 번에 Select 함
    // => Item 과 Register 도 ToOne 관계이므로
    // 조인해도 데이터 row 수가 증가하지않으므로 조인하여서 최적화함
    // => OrderItem, Item, Register 를 한 번에 Select 함

    /**
     * 전체 주문 조회
     * Dto 직접 조회 (일반 join) - v4
     * => Dto 로 직접 조회 시 Fetch join 불가능
     */
    public List<OrderQueryDto> findAllByDto_optimization() {
        List<OrderQueryDto> result = findOrders();

        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(toOrderIds(result));

        result.forEach(o -> o.setOrderItems(orderItemMap.get(o.getOrderId())));

        return result;
    }

    private List<OrderQueryDto> findOrders() {
        return em.createQuery(
                        "select new mybook.mymarket.repository.order.query." +
                                "OrderQueryDto(o.id, m.id, m.nickName, o.orderDate, o.status, d.status, d.type, d.address) " +
                                "from Order o " +
                                "join o.member m " +
                                "join o.deal d", OrderQueryDto.class)
                .getResultList();
    }

    public List<OrderQueryDto> findAllByStringByDto_optimization(OrderSearch orderSearch) {
        List<OrderQueryDto> result = findOrders(orderSearch);

        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(toOrderIds(result));

        result.forEach(o -> o.setOrderItems(orderItemMap.get(o.getOrderId())));

        return result;
    }

    // ToOne 관계 (M, D) => SQL 직접 조인 (fetch join 아님)
    private List<OrderQueryDto> findOrders(OrderSearch orderSearch) {
        String jpql = "select new mybook.mymarket.repository.order.query." +
                "OrderQueryDto(o.id, m.id, m.nickName, o.orderDate, o.status, d.status, d.type, d.address) " +
                "from Order o join o.member m join o.deal d";
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

        TypedQuery<OrderQueryDto> query = em.createQuery(jpql, OrderQueryDto.class)
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
     * 나의 주문 내역
     * Dto 직접 조회 (일반 join) - v4
     * => Dto 로 직접 조회 시 Fetch join 불가능
     */
    public List<OrderQueryDto> findMyAllByDto_optimization(Long memberId) {
        List<OrderQueryDto> result = findMyOrders(memberId);

        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(toOrderIds(result));

        result.forEach(o -> o.setOrderItems(orderItemMap.get(o.getOrderId())));

        return result;
    }

    private List<OrderQueryDto> findMyOrders(Long memberId) {
        return em.createQuery(
                        "select new mybook.mymarket.repository.order.query." +
                                "OrderQueryDto(o.id, m.id, m.nickName, o.orderDate, o.status, d.status, d.type, d.address) " +
                                "from Order o join o.member m join o.deal d " +
                                "where m.id = :memberId", OrderQueryDto.class)
                .setParameter("memberId", memberId)
                .getResultList();
    }

    private static List<Long> toOrderIds(List<OrderQueryDto> result) {
        List<Long> orderIds = result.stream()
                .map(o -> o.getOrderId())
                .collect(Collectors.toList());

        return orderIds;
    }

    private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long> orderIds) {
        // 루프를 돌리지않고 컬렉션을 한 번에 가져옴 => orderId 의 리스트를 이용
        // => orderId를 하나씩 하는 것이 아니라 In 절에 전부 포함 => in orderIds
        // => orderIds 에 대한 orderItems 들이 뽑혀져나옴
        List<OrderItemQueryDto> orderItems = em.createQuery(
                        "select new mybook.mymarket.repository.order.query." +
                                "OrderItemQueryDto(oi.order.id, r.member.id, i.id, i.name, r.member.nickName, oi.orderPrice, oi.count) " +
                                "from OrderItem oi " +
                                "join oi.item i " +
                                "join i.register r " +
                                "where oi.order.id in :orderIds", OrderItemQueryDto.class)
                .setParameter("orderIds", orderIds)  // 파라미터 바인딩
                .getResultList();

        // orderItems 을 그냥 써도 좋지만 한 번 더 최적화시켜줌 (Map 으로 바꾸는 것)
        // => 코드도 단순화시켜주고 성능도 최적화할 수 있게끔
        // key: orderId, value: orderItems 로 매핑시켜줌
        Map<Long, List<OrderItemQueryDto>> orderItemMap = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItemQueryDto::getOrderId));

//        람다식 표현
//        Map<Long, List<OrderItemQueryDto>> orderItemMap = orderItems.stream()
//                .collect(Collectors.groupingBy(orderItemQueryDto -> orderItemQueryDto.getOrderId()));

        return orderItemMap;
    }

    /**
     * Controller 계층에 이용하는 전체 주문 조회 로직
     * : v4와 비슷하지만 v4는 Dto 직접 조회, 여기서는 엔티티로 ToOne 관계 전부 Fetch join 하여
     * OrderDto, OrderItemDto 로 화면에 필요한 필드 생성하여 이용 => 필요한 Dto 에 따라 활용성이 뛰어남
     * OrderApiController 에서 v2, v3는 성능이 안좋음
     * => v2: toOne 관계에서 Proxy 초기화 => Fetch join (v3)
     * => v3: toOne 관계 모두 Fetch join 으로 N + 1 문제 해결
     * => 또한 orderItems 컬렉션의 경우 default_batch_fetch_size 로 문제 해결
     * 하지만 item - register (OneToOne 양방향연관관계)에서 N + 1 문제 발생
     * => register 가 영속성 컨텍스트에 존재하지 않으므로 계속 DB 에 쿼리가 나가게 됨
     * => orderItem - item (ManyToOne), item - register (OneToOne)
     * => ToOne 관계 직접 조인하여 해결 필요 (v4)
     */
    public List<OrderQueryDto> findAllByString_optimization(OrderSearch orderSearch) {
        // Order - Deal, Member => ToOne 관계 엔티티 Fetch join 하여 한 방 쿼리로 가져옴
        // where 문에서 검색될 조건들 포함시켜서
        List<Order> orders = findAllByString_fetch(orderSearch);

        // 가져온 쿼리로 화면에 맞는 Dto 로 생성
        List<OrderQueryDto> result = orders.stream().map(o -> new OrderQueryDto(o.getId(), o.getMember().getId(), o.getMember().getNickName(), o.getOrderDate(),
                o.getStatus(), o.getDeal().getStatus(), o.getDeal().getType(), o.getDeal().getAddress())).collect(Collectors.toList());

        // OrderItem - Item - Register => ToOne 관계 엔티티 Fetch join 하여 한 방 쿼리로 가져옴
        // => 위에 Dto 의 order_id(PK)값을 In 절에 전부 포함시킴 => in orderIds (Map 으로 바꾸는 것)
        // => 코드도 단순화시켜주고 성능도 최적화할 수 있게끔
        // key: orderId, value: orderItems 로 매핑시켜줌
        Map<Long, List<OrderItemQueryDto>> orderItemMap2 = findOrderItemMap2(toOrderIds(result));

        // orderIds 에 대한 orderItems 들이 뽑혀져나옴
        result.forEach(orderQueryDto -> orderQueryDto.setOrderItems(orderItemMap2.get(orderQueryDto.getOrderId())));

        return result;
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

    private Map<Long, List<OrderItemQueryDto>> findOrderItemMap2(List<Long> orderIds) {
        // orderIds 에 맞는 orderItemList 전부 가져옴 (엔티티 조회)
        List<OrderItem> orderItemList = findOrderItems_fetch2(orderIds);

        // 엔티티 List -> Dto List
        List<OrderItemQueryDto> orderItems = orderItemList.stream()
                .map(oi -> new OrderItemQueryDto(oi.getOrder().getId(), oi.getItem().getRegister().getId(), oi.getItem().getId(),
                        oi.getItem().getName(), oi.getItem().getRegister().getMember().getNickName(), oi.getOrderPrice(), oi.getCount()))
                .collect(Collectors.toList());

        // orderItems 을 그냥 써도 좋지만 한 번 더 최적화시켜줌 (Map 으로 바꾸는 것)
        // => 코드도 단순화시켜주고 성능도 최적화할 수 있게끔
        // key: orderId, value: orderItems 로 매핑시켜줌
        Map<Long, List<OrderItemQueryDto>> orderItemMap = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItemQueryDto::getOrderId));

//        람다식 표현
//        Map<Long, List<OrderItemQueryDto>> orderItemMap = orderItems.stream()
//                .collect(Collectors.groupingBy(orderItemQueryDto -> orderItemQueryDto.getOrderId()));
        return orderItemMap;
    }

    public List<OrderItem> findOrderItems_fetch2(List<Long> orderIds) {
        List<OrderItem> orderItems = em.createQuery(
                      "select oi from OrderItem oi " +
                                "join fetch oi.item i " +
                                "join fetch i.register r " +
                                "join fetch r.member m " +
                                "where oi.order.id in :orderIds", OrderItem.class)
                .setParameter("orderIds", orderIds)  // 파라미터 바인딩
                .getResultList();

        return orderItems;
    }


    /**
     * Controller 계층에 이용하는 나의 주문 내역 로직
     */
    public List<OrderQueryDto> findMyOrders_optimization(Long memberId) {
        List<Order> myOrders = findMyOrders_fetch(memberId);

        List<OrderQueryDto> result = myOrders.stream().map(o -> new OrderQueryDto(o.getId(), o.getMember().getId(), o.getMember().getNickName(), o.getOrderDate(),
                o.getStatus(), o.getDeal().getStatus(), o.getDeal().getType(), o.getDeal().getAddress())).collect(Collectors.toList());

        Map<Long, List<OrderItemQueryDto>> orderItemMap2 = findOrderItemMap2(toOrderIds(result));

        result.forEach(orderQueryDto -> orderQueryDto.setOrderItems(orderItemMap2.get(orderQueryDto.getOrderId())));

        return result;
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
}
