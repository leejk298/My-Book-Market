package mybook.mymarket.service;

import mybook.mymarket.controller.dto.ItemDto;
import mybook.mymarket.domain.*;
import mybook.mymarket.domain.item.Item;
import mybook.mymarket.domain.item.Novel;
import mybook.mymarket.exception.NotEnoughStockException;
import mybook.mymarket.repository.ItemRepository;
import mybook.mymarket.repository.OrderRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.Assert.*;


@SpringBootTest     // 스프링 위에서 테스트
@RunWith(SpringRunner.class)    // 스프링 컨테이너 안에서 실행
@Transactional      // 데이터 변겅이 일어나므로, 롤백시키기 위해
public class OrderServiceTest {
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    OrderService orderService;
    @Autowired
    RegisterService registerService;
    @Autowired
    ItemRepository itemRepository;
    @Autowired
    EntityManager em;

    /**
     * 상품 주문
     */
    @Test
    public void 상품주문() throws Exception {
        // given: 회원, 상품 세팅, 등록
        // 등록회원
        Address registerAddress = new Address("a", "b", "c");
        Member registerMember = createMember("testMember", registerAddress);
        em.persist(registerMember);

        // 주문회원
        Address orderAddress = new Address("1", "2", "3");
        Member orderMember = createMember("orderMember", orderAddress);
        em.persist(orderMember);

        // 상품 등록
        String itemType = "Novel";  // 상품분류 - 라디오버튼으로 가져옴
        int price = 10000, registerStockQuantity = 10;
        Novel novel = createNovel(price, registerStockQuantity); // item 세팅 아직 영속 X
        ItemDto itemDto = new ItemDto(novel);   // 엔티티 -> Dto
        Long registerId = registerService.register(registerMember.getId(), itemDto, itemType, registerStockQuantity);// item 영속 O

        // when: 상품 주문, 거래
        String dealType = "DIRECT"; // 직거래 -> 등록회원 주소
        int orderStockQuantity = 10;
        Long orderId = orderService.order(orderMember.getId(), registerId, orderStockQuantity, dealType);

        // then: 검증해야할 대상
        Order order = orderRepository.findOne(orderId);
        Register register = registerService.findOne(registerId);
        Item item = itemRepository.findOne(register.getItem().getId());

        assertEquals("상품 주문 시 상태는 ORDER", OrderStatus.ORDER, order.getStatus());
        assertEquals("상품 주문 시 거래 상태는 WAIT", DealStatus.WAIT, order.getDeal().getStatus());
        assertEquals("직거래 시 주소는 등록회원 주소", registerMember.getAddress(), order.getDeal().getAddress());
        assertEquals("직거래 시 거래 타입은 DIRECT", DealType.DIRECT, order.getDeal().getType());
        assertEquals("주문 상품 가격", price * orderStockQuantity, order.getTotalPrice());
        assertEquals("주문 회원 이름", orderMember.getNickName(), order.getMember().getNickName());
        assertEquals("상품 재고", registerStockQuantity - orderStockQuantity, item.getStockQuantity());
        assertEquals("상품 재고가 0 이면 등록상태는 CANCEL", RegisterStatus.CANCEL, register.getStatus());
    }

    @Test(expected = NotEnoughStockException.class)
    public void 상품주문_재고예외() throws Exception {
        // given: 회원, 상품 세팅, 등록
        // 등록회원
        Address registerAddress = new Address("a", "b", "c");
        Member registerMember = createMember("testMember", registerAddress);
        em.persist(registerMember);

        // 주문회원
        Address orderAddress = new Address("1", "2", "3");
        Member orderMember = createMember("orderMember", orderAddress);
        em.persist(orderMember);

        // 상품 등록
        String itemType = "Novel";  // 상품분류 - 라디오버튼으로 가져옴
        int price = 10000, registerStockQuantity = 10;
        Novel novel = createNovel(price, registerStockQuantity); // item 세팅 아직 영속 X
        ItemDto itemDto = new ItemDto(novel);   // 엔티티 -> Dto
        Long registerId = registerService.register(registerMember.getId(), itemDto, itemType, registerStockQuantity);// item 영속 O

        // when
        String dealType = "DIRECT";     // 직거래 -> 등록회원 주소
        int orderStockQuantity = 11;    // 재고보다 많은 주문수량 => 예외 발생
        orderService.order(orderMember.getId(), registerId, orderStockQuantity, dealType);  // "need more stock"

        // then
        Assert.fail("재고는 음수가 안되므로 예외가 발생해야 한다.");  // 여기로 오면 잘못 작성한 테스트 케이스
    }

    /**
     * 주문 취소
     */
    @Test
    public void 주문취소() throws Exception {
        // given: 회원, 상품 세팅, 등록
        // 등록회원
        Address registerAddress = new Address("a", "b", "c");
        Member registerMember = createMember("testMember", registerAddress);
        em.persist(registerMember);

        // 주문회원
        Address orderAddress = new Address("1", "2", "3");
        Member orderMember = createMember("orderMember", orderAddress);
        em.persist(orderMember);

        // 상품 등록
        String itemType = "Novel";  // 상품분류 - 라디오버튼으로 가져옴
        int price = 10000, registerStockQuantity = 10;
        Novel novel = createNovel(price, registerStockQuantity); // item 세팅 아직 영속 X
        ItemDto itemDto = new ItemDto(novel);   // 엔티티 -> Dto
        Long registerId = registerService.register(registerMember.getId(), itemDto, itemType, registerStockQuantity);// item 영속 O

        String dealType = "DELIVERY";   // 배송 -> 주문 회원 주소
        int orderStockQuantity = 5;
        Long orderId = orderService.order(orderMember.getId(), registerId, orderStockQuantity, dealType);

        // when
        Register register = registerService.findOne(registerId);
        Item item = itemRepository.findOne(register.getItem().getId());
        orderService.cancelOrder(orderId);
        Order order = orderRepository.findOne(orderId);

        // then
        assertEquals("주문 취소 시 상태는 CANCEL", OrderStatus.CANCEL, order.getStatus());
        assertEquals("주문 취소 시 재고는 원래대로", registerStockQuantity, item.getStockQuantity());
    }

    /**
     * 거래 확정
     */
    @Test
    public void 거래확정() throws Exception {
        // given: 회원, 상품 세팅, 등록
        // 등록회원
        Address registerAddress = new Address("a", "b", "c");
        Member registerMember = createMember("testMember", registerAddress);
        em.persist(registerMember);

        // 주문회원
        Address orderAddress = new Address("1", "2", "3");
        Member orderMember = createMember("orderMember", orderAddress);
        em.persist(orderMember);

        // 상품 등록
        String itemType = "Novel";  // 상품분류 - 라디오버튼으로 가져옴
        int price = 10000, registerStockQuantity = 10;
        Novel novel = createNovel(price, registerStockQuantity); // item 세팅 아직 영속 X
        ItemDto itemDto = new ItemDto(novel);   // 엔티티 -> Dto
        Long registerId = registerService.register(registerMember.getId(), itemDto, itemType, registerStockQuantity);// item 영속 O

        Register register = registerService.findOne(registerId);
        Item item = itemRepository.findOne(register.getItem().getId());

        // when
        String dealType = "DELIVERY";   // 배송 -> 주문 회원 주소
        int orderStockQuantity = 7;
        Long orderId = orderService.order(orderMember.getId(), registerId, orderStockQuantity, dealType);
        Order order = orderRepository.findOne(orderId);

        // then
        order.getDeal().setStatus(DealStatus.COMP); // 거래 완료 -> 상태 업데이트

        assertEquals("배송 거래 시 거래 타입은 DELIVERY", DealType.DELIVERY, order.getDeal().getType());
        assertEquals("배송 거래 시 주소는 주문회원 주소", orderMember.getAddress(), order.getDeal().getAddress());
        assertEquals("상품 재고", registerStockQuantity - orderStockQuantity, item.getStockQuantity());
        assertEquals("거래 완료 시 거래 상태는 COMP", DealStatus.COMP, order.getDeal().getStatus());
    }

    private static Novel createNovel(int price, int stockQuantity) {
        Novel novel = new Novel();
        novel.setName("testBook");
        novel.setAuthor("test");
        novel.setPrice(price);
        novel.setStockQuantity(stockQuantity);
        novel.setGenre("test");

        return novel;
    }

    private static Member createMember(String nickName, Address address) {
        Member member = new Member(nickName, "1234", "이정규", address);

        return member;
    }
}