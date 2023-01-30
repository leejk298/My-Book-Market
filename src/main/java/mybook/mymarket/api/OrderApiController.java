package mybook.mymarket.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import mybook.mymarket.controller.dto.OrderDto;
import mybook.mymarket.domain.*;
import mybook.mymarket.repository.OrderRepository;
import mybook.mymarket.repository.OrderSearch;
import mybook.mymarket.repository.order.query.OrderQueryDto;
import mybook.mymarket.repository.order.query.OrderQueryRepository;
import mybook.mymarket.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController // @RestController == @Controller + @ResponseBody
// @ResponseBody : data 자체를 바로 Json 이나 XML 로 바로 보내기 위해
@RequiredArgsConstructor // final 키워드를 가진 필드(orderRepository)로 생성자를 만들어줌
public class OrderApiController {
    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;
    private final OrderService orderService;

    /**
     * 상품 주문
     */
    @PostMapping("/api/order/{id}")  // id를 pathVariable 로 가져옴
    public ResponseData<OrderDto> createOrder(@PathVariable("id") Long memberId,
                                              @RequestBody UserRequest request) {
        // 주문 시에는 어떤 회원이 구매하는지 정보가 필요하므로 memberId
        // 추가로 어떠한 등록 상품을 구매할 것인지 정보가 필요하므로 registerId
        // 상품을 몇 개 주문, 거래정보는 어떻게 할 것인지(직거래, 배송)
        Long orderId = orderService.order(memberId, request.registerId, request.count, request.type);

        // Json 데이터를 보여주기 위한 로직
        Order order = orderRepository.findOrderMember(orderId);
        OrderDto orderDto = new OrderDto(order);

        // 등록하여 반환된 registerDto 를 Json 형식으로 보여줌
        return new ResponseData<>(orderDto);
    }

    /**
     * 주문 취소
     */
    @GetMapping("/api/orders/cancel/{id}")
    public ResponseData<OrderDto> cancelOrder(@PathVariable("id") Long orderId) {
        // 로그인을 하면 회원 정보를 세션에 저장하므로 이미 로그인된 상태로 가정
        /** 커맨드와 쿼리를 분리하자 */
        // 커맨드: update 같은 변경성 메소드는 void 로 끝내거나 id값 정도만 반환함(찾기 위해)
        orderService.cancelOrder(orderId);
        // 주문을 취소하게 되면 상품 재고는 원복되고, 주문 상태는 CANCEL
        // 주문 시 재고가 0이되었다가 해당 주문을 취소하게되면
        // 등록 상태는 REGISTER => 주문도 가능하게 됨
        // 또한 주문의 거래상태가 COMP(거래완료)이면 취소 불가능
        // => NotCorrectAccess("올바른 접근이 아닙니다.") 예외 발생

        // 쿼리: 그 후에 별도로 쿼리를 짠다
        // Json 데이터를 보여주기 위한 로직
        Order order = orderRepository.findOrderMember(orderId);
        OrderDto orderDto = new OrderDto(order);

        // 등록하여 반환된 registerDto 를 Json 형식으로 보여줌
        return new ResponseData<>(orderDto);
    }

    /**
     * 거래 완료
     */
    @GetMapping("/api/orders/complete/{id}")
    public ResponseData<OrderDto> completeOrderDeal(@PathVariable("id") Long orderId) {
        // 로그인을 하면 회원 정보를 세션에 저장하므로 이미 로그인된 상태로 가정
        /** 커맨드와 쿼리를 분리하자 */
        // 커맨드: update 같은 변경성 메소드는 void 로 끝내거나 id값 정도만 반환함(찾기 위해)
        orderService.completeDeal(orderId);
        // 주문 거래를 완료하게 되면 해당 주문, 상품 수정은 불가능하며, 거래 상태는 COMP
        // 또한 주문 상태가 CANCEL(취소)이면 거래완료 불가능
        // => NotCorrectAccess("올바른 접근이 아닙니다.") 예외 발생

        // 쿼리: 그 후에 별도로 쿼리를 짠다
        // Json 데이터를 보여주기 위한 로직
        /**
         * 특정 주문과 관련된 주문상품, 상품, 등록, 회원 정보를 가져옴
         * => OI - I (ToOne 관계), I - R (ToOne 관계), R - M (ToOne 관계)
         * => Fetch join: 엔티티 영속화
         */
        orderRepository.findOrderItems_fetch(orderId);
        Order order = orderRepository.findOrderMember(orderId);
        OrderDto orderDto = new OrderDto(order);

        // 등록하여 반환된 registerDto 를 Json 형식으로 보여줌
        return new ResponseData<>(orderDto);
    }

    @Data
    @NoArgsConstructor
    static class UserRequest {
        Long registerId;  // 등록회원, 상품 정보 필요
        int count;  // 주문 수량
        String type; // 거래 타입
    }

    @Data
    @AllArgsConstructor
    static class ResponseData<T> {
        private T data;
    }

    /**
     * 전체 주문 조회
     */

    /**
     * v2: 일반 Join - 엔티티
     * 연관 엔티티에 일반 join 을 하게되면 Select 대상의 엔티티는 영속화하여 가져오지만,
     * 조인의 대상은 영속화하여 가져오지 않는다.
     * 연관 엔티티가 검색 조건에 포함되고, 조회의 주체가 검색 엔티티뿐일 때 사용하면 좋다
     * => member, deal 에 대한 Proxy 객체 초기화 => Lazy 로딩에 의한 DB 쿼리가 너무 많이 나감
     * => order 1번 - member, deal N번(order 조회수만큼) - orderItem N번(order 조회수만큼)
     * => orderItem N번 - item N번(orderItem 조회수만큼) - register M번(item 조회수만큼)
     * => item N번 - register N번(item 조회수만큼) => 매우 심각
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
        List<Order> orders = orderRepository.findAllWithMemberDeal();

        // 엔티티 List -> Dto List
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());

        return new Result<>(result.size(), result);
    }
    /**
     * 전체 주문 조회 시 @GetMapping 과 @PostMapping 차이
     * @GetMapping: 전체 주문 조회, @PostMapping: @RequestBody 로 넘긴 조건에 맞는 주문 조회
     * @RequestBody: Json 으로 온 OrderSearch(data)를 넘김
     * OrderSearch: where 문에서 검색될 조건들을 만족하는 주문 조회
     */
    @PostMapping("/api/v2/orders")
    public Result<List<OrderDto>> ordersV2(@RequestBody OrderSearch orderSearch) {
        // Lazy 로딩에 의한 DB 쿼리가 너무 많이 나감
        // 주문상품에 대해 같은 상품이라면 상품에 대한 Lazy 로딩이 한 번 일어나겠지만
        // 최악의 경우로 생각 => 영속성 컨텍스트에 존재하지 않으면 계속 DB 에 쿼리가 나가므로
        // => 상당히 많은 쿼리가 나감 => 최적화 필요 => Fetch join
        // => 컬렉션인 경우 Fetch join 할 때 고민해야할 포인트가 많음 => V3
        // 엔티티를 조회해옴 => 실무에선 페이징으로 처리
        List<Order> orders = orderRepository.findAllWithMemberDealByString(orderSearch);

        // 엔티티 List -> Dto List
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());

        return new Result<>(result.size(), result);
    }

    /**
     * v3: Fetch Join - 엔티티
     * 연관 엔티티에 fetch join 을 하게되면 select 대상의 엔티티 뿐만 아니라 조인의 대상까지 영속화하여 가져온다.
     * 연관 엔티티까지 select 의 대상일 때, N + 1의 문제를 해결하여 가져올 수 있는 좋은 방법이다.
     * v2랑 v3는 로직은 같지만 쿼리가 완전히 다름, Fetch join: 한방쿼리
     * 한 번에 다 끌고와서 엔티티 -> Dto 로 변환하는 단점이 있음 => 최적화: 직접 Dto 이용(v4)
     * Order 를 기준으로 X To One 은 fetch join 을 하고
     * => ToOne 은 데이터 뻥튀기가 안되므로 한 번에 끌고와야 함
     * => Member(다대일), Delivery(일대일) 이므로 한방쿼리로 나옴
     * 컬렉션의 경우 => Lazy 강제 초기화 => Lazy 로딩 최적화 => N + 1 문제 해결 필요
     * => default_batch_fetch_size: 100 # where 절에서 IN 쿼리의 개수 => id 개수
     * => OrderItem 을 컬렉션 개수만큼 한 번에 당겨오는 것
     * => 1 + N + M => 1 + 1 + 1 로 되어버림
     * => 조인보다 DB 데이터 전송량이 최적화 됨
     * 하지만 item - register (OneToOne 양방향연관관계)에서 N + 1 문제 발생
     * => register 가 영속성 컨텍스트에 존재하지 않으므로 계속 DB 에 쿼리가 나가게 됨
     * => orderItem - item (ManyToOne), item - register (OneToOne) => ToOne 관계 직접 조인하여 해결 필요 (v4)
     */
    @GetMapping("/api/v3/orders")
    public Result<List<OrderDto>> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDeal_fetch();

        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());
        // Object 타입 {...}으로 반환, Result 라는 껍데기를 씌어서 data 필드의 값은 List 가 나가게됨
        // Object 타입으로 반환하지 않으면 배열타입 [...] 으로 나가게됨 => 확장성, 유연성 X
        return new Result<>(result.size(), result);
    }

    @PostMapping("/api/v3/orders")
    public Result<List<OrderDto>> ordersV3(@RequestBody OrderSearch orderSearch) {
        List<Order> orders = orderRepository.findAllWithMemberDealByString_fetch(orderSearch);

        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());
        // Object 타입 {...}으로 반환, Result 라는 껍데기를 씌어서 data 필드의 값은 List 가 나가게됨
        // Object 타입으로 반환하지 않으면 배열타입 [...] 으로 나가게됨 => 확장성, 유연성 X
        return new Result<>(result.size(), result);
    }

    /**
     * v4: 일반 Join - Dto
     * : ToOne 관계(M, D)들 OrderQueryDto 로 직접 조회 먼저 조회하고 => findOrders()
     * => 여기서 얻은 식별자 orderId로 ToMany 관계인 OrderItem 을 한꺼번에 조회
     * => orderId를 하나씩 하는 것이 아니라 In 절에 전부 포함 => in orderIds
     * => orderIds 에 대한 orderItems 들이 뽑혀져나옴
     * 또한 orderItem - item - register => ToOne 관계 직접 조인하여 한꺼번에 조회
     * => ToOne 관계(I, R)들 OrderItemQueryDto 로 직접 조회 - 일반 join (Fetch join 불가능)
     * orderItems 을 그냥 써도 좋지만 한 번 더 최적화시켜줌 (Map 으로 바꾸는 것)
     * => 코드도 단순화시켜주고 성능도 최적화할 수 있게끔
     * 성능 최적화 => Map 을 사용해서 매칭 성능 향상: O(1)의 시간복잡도
     * => key: orderId, value: orderItems 로 매핑시켜줌
     * tradeoff => Fetch 조인(V3)보다 쿼리를 직접 작성하는 양도 많지만,
     * 장점은 Fetch 조인보다 확실히 데이터를 select 한 양이 줄어듦
     */
    @GetMapping("/api/v4/orders")
    public Result<List<OrderQueryDto>> ordersV4() {
        List<OrderQueryDto> allByDtoOptimization = orderQueryRepository.findAllByDto_optimization();

        return new Result<>(allByDtoOptimization.size(), allByDtoOptimization);
    }

    @PostMapping("/api/v4/orders")
    public Result<List<OrderQueryDto>> ordersV4(@RequestBody OrderSearch orderSearch) {
        List<OrderQueryDto> allByDtoOptimization = orderQueryRepository.findAllByStringByDto_optimization(orderSearch);

        return new Result<>(allByDtoOptimization.size(), allByDtoOptimization);
    }

    /**
     * 쿼리 방식 선택 권장 순서 (V3 <-> V4)
     1. 우선 엔티티를 DTO 로 변환하는 방법을 선택(V2) -> 코드 유지보수성 좋음
     2. 필요하면 Fetch join 으로 성능을 최적화한다(V3) => Web Order 조회 구현방식

     => Order 조회의 경우에는 Fetch join 을 하여도 컬렉션인 OrderItem 에 해당하는 item, register 가
     => N + 1 문제 발생 => 컬렉션의 경우 default_batch_fetch_size 로 문제 해결
     => Web 에서의 Order 조회는 Order - M, D 와 OrderItem - I, R 각각을 Fetch join 하여
     => order_id(PK)값을 In 절에 전부 포함시켜 (default_batch_fetch_size) 해결함
     => API 에서의 Order 조회는 Order - M, D 와 OrderItem - I, R 각각을 Dto 직접 조회 - 일반 join 하여
     => order_id(PK)값을 In 절에 전부 포함시켜 (default_batch_fetch_size) 해결함
     => Web 은 엔티티 Fetch join, API 는 Dto 직접 조회 - 일반 join (Fetch join 불가능)

     3. 그래도 안되면 DTO 로 직접 조회하는 방법을 사용(V4) => API Order 조회 구현방식
     4. 최후의 방법은 JPA 가 제공하는 네이티브 SQL 이나 SpringJDBCTemplate 을 사용해서 SQL 직접 사용
     */

    /**
     * 나의 주문 조회
     */
    @GetMapping("/api/v2/myOrders/{id}")
    public Result<List<OrderDto>> myOrdersV2(@PathVariable("id") Long memberId) {
        List<Order> myOrders = orderRepository.findMyOrders(memberId);

        List<OrderDto> result = myOrders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());

        return new Result<>(result.size(), result);
    }

    @GetMapping("/api/v3/myOrders/{id}")
    public Result<List<OrderDto>> myOrdersV3(@PathVariable("id") Long memberId) {
        List<Order> myOrders = orderRepository.findMyOrders_fetch(memberId);

        List<OrderDto> result = myOrders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());

        return new Result<>(result.size(), result);
    }

    @GetMapping("/api/v4/myOrders/{id}")
    public Result<List<OrderQueryDto>> myOrdersV4(@PathVariable("id") Long memberId) {
        List<OrderQueryDto> myAllByDtoOptimization = orderQueryRepository.findMyAllByDto_optimization(memberId);

        return new Result<>(myAllByDtoOptimization.size(), myAllByDtoOptimization);
    }

    @Data
    @AllArgsConstructor // 필드 값을 전부 포함하는 생성자를 만들어줌
    static class Result<T> {    // Object 타입, 한 번 감싸서 반환
        private int count;  // 개수
        private T data; // 제네릭 타입
    }
}
