package mybook.mymarket.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import mybook.mymarket.controller.dto.OrderDto;
import mybook.mymarket.domain.*;
import mybook.mymarket.repository.OrderRepository;
import mybook.mymarket.repository.OrderSearch;
import mybook.mymarket.repository.order.query.OrderQueryDto;
import mybook.mymarket.repository.order.query.OrderQueryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController // @RestController == @Controller + @ResponseBody
// @ResponseBody : data 자체를 바로 Json 이나 XML 로 바로 보내기 위해
@RequiredArgsConstructor // final 키워드를 가진 필드(orderRepository)로 생성자를 만들어줌
public class OrderApiController {
    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /**
     * 주문 조회
     */

    /**
     * v2: 일반 Join - 엔티티
     * : Lazy 로딩에 의한 DB 쿼리가 너무 많이 나감
     * => order 1번 - member, deal N번(order 조회수만큼) - orderItem N번(order 조회수만큼)
     * => orderItem N번 - item N번(orderItem 조회수만큼) - register N번(item 조회수만큼)
     * => Eager 로 해도 해결 X: 예상 밖의 조인쿼리가 나감 (양방향 연관관계때문에), 예측 불가 => 성능 X
     * => 영속성 컨텍스트에 존재하지 않으면 계속 DB 에 쿼리가 나가므로
     * => 상당히 많은 쿼리가 나감 => 최적화 필요 => Fetch join(v3)
     */
    @GetMapping("/api/v2/orders")
    public Result<List<OrderDto>> ordersV2() {
        // Lazy 로딩에 의한 DB 쿼리가 너무 많이 나감
        // 주문상품에 대해 같은 상품이라면 상품에 대한 Lazy 로딩이 한 번 일어나겠지만
        // 최악의 경우로 생각 => 영속성 컨텍스트에 존재하지 않으면 계속 DB 에 쿼리가 나가므로
        // => 상당히 많은 쿼리가 나감 => 최적화 필요 => Fetch join
        // => 컬렉션인 경우 Fetch join 할 때 고민해야할 포인트가 많음 => V3
        // 엔티티를 조회해옴 => 실무에선 페이징으로 처리
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());

        // 엔티티 List -> Dto List
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());

        return new Result<>(result.size(), result);
    }

    /**
     *  v3: Fetch Join - 엔티티
     *  : v2랑 v3는 로직은 같지만 쿼리가 완전히 다름, Fetch join: 한방쿼리
     *  한 번에 다 끌고와서 엔티티 -> Dto 로 변환하는 단점이 있음 => 최적화: 직접 Dto 이용(v4)
     *  Order 를 기준으로 X To One 은 fetch join 을 하고
     *  => ToOne 은 데이터 뻥튀기가 안되므로 한 번에 끌고와야 함
     *  => Member(다대일), Delivery(일대일) 이므로 한방쿼리로 나옴
     *  일대다 컬렉션의 경우 => Lazy 강제 초기화 => Lazy 로딩 최적화 => N + 1 문제 해결 필요
     *  => default_batch_fetch_size: 100 # where 절에서 IN 쿼리의 개수 => id 개수
     *  => OrderItem 을 컬렉션 개수만큼 한 번에 당겨오는 것
     *  => 1 + N + M => 1 + 1 + 1 로 되어버림
     *  => 조인보다 DB 데이터 전송량이 최적화 됨
     *  하지만 item - register (OneToOne 양방향연관관계)에서 N + 1 문제 발생
     *  => register 가 영속성 컨텍스트에 존재하지 않으므로 계속 DB 에 쿼리가 나가게 됨
     *  => orderItem - item (ManyToOne), item - register (OneToOne) => ToOne 관계 직접 조인하여 해결 필요 (v4)
     */
    @GetMapping("/api/v3/orders")
    public Result<List<OrderDto>> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDeal();

        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());
        // Object 타입 {...}으로 반환, Result 라는 껍데기를 씌어서 data 필드의 값은 List 가 나가게됨
        // Object 타입으로 반환하지 않으면 배열타입 [...] 으로 나가게됨 => 확장성, 유연성 X
        return new Result<>(result.size(), result);
    }

    /**
     * v4: 일반 Join - Dto
     * : ToOne 관계(M, D)들은 먼저 조회하고 => findOrders()
     * => 여기서 얻은 식별자 orderId로 ToMany 관계인 OrderItem 을 한꺼번에 조회
     * => orderId를 하나씩 하는 것이 아니라 In 절에 전부 포함 => in orderIds
     * => orderIds 에 대한 orderItems 들이 뽑혀져나옴
     * 또한 orderItem - item - register => ToOne 관계 직접 조인하여 한꺼번에 조회
     * orderItems 을 그냥 써도 좋지만 한 번 더 최적화시켜줌 (Map 으로 바꾸는 것)
     * => 코드도 단순화시켜주고 성능도 최적화할 수 있게끔
     * 성능 최적화 => Map 을 사용해서 매칭 성능 향상, O(1)
     * => key: orderId, value: orderItems 로 매핑시켜줌
     * tradeoff => Fetch 조인(V3)보다 쿼리를 직접 작성하는 양도 많지만,
     * 장점은 Fetch 조인보다 확실히 데이터를 select 한 양이 줄어듦
     */
    @GetMapping("/api/v4/orders")
    public Result<List<OrderQueryDto>> ordersV4() {
        List<OrderQueryDto> allByDtoOptimization = orderQueryRepository.findAllByDto_optimization();

        return new Result<>(allByDtoOptimization.size(), allByDtoOptimization);
    }

    /**
     * 쿼리 방식 선택 권장 순서 (V3 <-> V4)
     1. 우선 엔티티를 DTO 로 변환하는 방법을 선택(V2) -> 코드 유지보수성 좋음
     2. 필요하면 Fetch join 으로 성능을 최적화한다(V3) -> 대부분의 성능이슈 해결가능, 90 %
     3. 그래도 안되면 DTO 로 직접 조회하는 방법을 사용(V4)
     4. 최후의 방법은 JPA 가 제공하는 네이티브 SQL 이나 SpringJDBCTemplate 을 사용해서 SQL 직접 사용
     */

    @Data
    @AllArgsConstructor // 필드 값을 전부 포함하는 생성자를 만들어줌
    static class Result<T> {    // Object 타입, 한 번 감싸서 반환
        private int count;  // 개수
        private T data; // 제네릭 타입
    }
}
