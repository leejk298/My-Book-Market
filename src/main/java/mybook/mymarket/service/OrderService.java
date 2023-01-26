package mybook.mymarket.service;

import lombok.RequiredArgsConstructor;

import mybook.mymarket.domain.*;
import mybook.mymarket.domain.item.Item;
import mybook.mymarket.repository.ItemRepository;
import mybook.mymarket.repository.OrderRepository;
import mybook.mymarket.repository.MemberRepository;
import mybook.mymarket.repository.RegisterRepository;
import mybook.mymarket.repository.OrderSearch;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service    // 스프링빈에 등록
@Transactional(readOnly = true) // 읽기전용: 리소스 낭비 X, default: false
@RequiredArgsConstructor    // final 키워드를 가진 필드(orderRepository, memberRepository, itemRepository)로 생성자를 만들어줌
public class OrderService {
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;    // 값 세팅을 위해 의존관계 주입
    private final ItemRepository itemRepository; // 값 세팅을 위해 의존관계 주입
    private final RegisterRepository registerRepository;

    /**
     * 주문 => 데이터 변경 필요 => Transactional
     */
    @Transactional
    public Long order(Long memberId, Long registerId, int count, String type) {
        // 상품 id, 수량만 넘어오게 => 값을 꺼내야 함 => 해당 repository 필요 => 주입받아 사용 => 주입받기 위해 생성자 injection

        //엔티티 조회
        Member member = memberRepository.findOne(memberId);     // 회원
        Register register = registerRepository.findOne(registerId); // 등록 => 배송지 주소, 등록상품조회에 필요
        Item item = itemRepository.findOne(register.getItem().getId()); // 등록 상품

        /**
         * 거래 형태
         * 배송 => 구매자 주소
         * 직거래 => 판매자 주소
         */
        // 거래정보 생성
        Deal deal = new Deal();
        if (DealType.DELIVERY.toString().equals(type)) {    // 거래 형태가 배송인 경우
            deal.setAddress(member.getAddress());   // 거래 주소를 주문 회원의 주소로 설정
            deal.setType(DealType.DELIVERY);       // 거래 형태 세팅
        } else {    // 거래 형태가 직거래인 경우
            deal.setAddress(register.getMember().getAddress()); // 거래 주소를 등록 회원의 주소로 설정
            deal.setType(DealType.DIRECT);  // 거래 형태 세팅
        }

        deal.setStatus(DealStatus.WAIT);    // 거래 상태 WAIT

        // 주문상품 생성 => 생성 메소드 이용(static)
        OrderItem orderItem = OrderItem.createOrderItem(item, item.getPrice(), count);

        // 주문 생성 => 생성 메소드 이용(static)
        Order order = Order.createOrder(member, deal, orderItem);

        // 주문 저장
        /** dealRepository 에도 save + orderItemRepository 에도 save 하고
         orderRepository 에 save 해야 하지만, cascade 옵션으로 orderRepository
         하나에만 save 해도 전부 save => persist 가 된다
         - Order 만 delivery, orderItem 을 관리하는 그림에서만 사용하자
         즉, 다른 곳에서 deal, orderItem 을 사용하지 않으므로 가능한 것 */
        orderRepository.save(order);

        return order.getId();
    }

    /**
     * 거래 확정 => 데이터 변경 필요 => Transactional
     */
    @Transactional
    public void completeDeal(Long orderId) {    // 거래 완료
        // 엔티티 조회
        Order order = orderRepository.findOne(orderId); // 해당 주문 가져와서

        // 거래 완료
        order.completeDeal();   // 해당 주문의 거래 상태 업데이트 => 변경감지
    }

    /**
     * 주문 취소 => 데이터 변경 필요 => Transactional
     */
    @Transactional
    public void cancelOrder(Long orderId) { // 취소 시 id 값만 넘어옴 => 찾아야함 => 엔티티 조회
        // 엔티티 조회
        Order order = orderRepository.findOne(orderId); // 값 가져옴

        // 주문 취소
        order.cancel(); // 해당 주문의 주문 상품 -> 상품 -> 등록 상태까지 변경감지

        /** 도메인 모델 패턴: 서비스 계층은 단순히 엔티티에 필요한 요청을 위암하는 역할
         엔티티가 비지니스 로직을 가지고 객체 지향의 특성을 적극 활용하는 것.
         그러므로 간단함 => 이미 엔티티에 비지니스 로직을 구현하였므로
         => 서비스 계층에서 취소 => Order 에 cancel() => OrderItem 에 cancel() => Item 에 addStock()

         * 반대로 엔티티에는 비지니스 로직이 거의 없고 서비스 계층에서 대부분의 비지니스
         로직을 처리하는 것을 트랜잭션 스크립트 패턴이라고 한다.

         * JPA 장점: 엔티티 안에 있는 데이터(OrderStatus, OrderItem.count)가 바뀌게 되면
         JPA 가 알아서 더티체킹(변경내역 감지)을 통해 변경된 내역들을 다 찾아서 DB에
         업데이트 쿼리가 전부 날라감 */
    }

    /**
     * 검색
     * Repository 에서 단순히 위임만 받으므로 굳이 서비스 계층을 안타도 됨
     */
    public List<Order> findOrders(OrderSearch orderSearch) {    // Where 절의 조건
        return orderRepository.findAllByString_fetch(orderSearch);    // 회원명, 거래상태, 주문상테
//        return orderRepository.findAll(orderSearch);
    }

    public List<Order> findMyOrders(Long memberId) {
        return orderRepository.findMyOrders_fetch(memberId);
    }
}
