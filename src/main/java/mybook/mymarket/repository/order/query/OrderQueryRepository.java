package mybook.mymarket.repository.order.query;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
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
    public List<OrderQueryDto> findAllByDto_optimization() {
        List<OrderQueryDto> result = findOrders();

        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(toOrderIds(result));

        result.forEach(o -> o.setOrderItems(orderItemMap.get(o.getOrderId())));

        return result;
    }

    // ToOne 관계 (M, D) => SQL 직접 조인 (fetch join 아님)
    private List<OrderQueryDto> findOrders() {
        return em.createQuery(
                        "select new mybook.mymarket.repository.order.query." +
                                "OrderQueryDto(o.id, m.nickName, o.orderDate, o.status, d.status, d.address) " +
                                "from Order o " +
                                "join o.member m " +
                                "join o.deal d", OrderQueryDto.class)
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
                                "OrderItemQueryDto(oi.order.id, i.id, i.name, r.member.nickName, oi.orderPrice, oi.count) " +
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
}
