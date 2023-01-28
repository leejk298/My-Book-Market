package mybook.mymarket.controller;


import lombok.RequiredArgsConstructor;
import mybook.mymarket.controller.form.DeliveryForm;
import mybook.mymarket.domain.Member;
import mybook.mymarket.domain.Register;
import mybook.mymarket.service.dto.MemberDto;
import mybook.mymarket.controller.dto.RegisterDto;
import mybook.mymarket.repository.OrderRepository;
import mybook.mymarket.repository.OrderSearch;
import mybook.mymarket.repository.order.query.OrderQueryDto;
import mybook.mymarket.service.MemberService;
import mybook.mymarket.service.OrderService;
import mybook.mymarket.service.RegisterService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class OrderController {
    // 주문하기 위해서 dependency 가 필요함 => 주입
    private final OrderService orderService;
    // 서비스 계층에서 단순히 위임만 하므로 바로 레포지토리 계층으로
    private final OrderRepository orderRepository;
    private final MemberService memberService;
    private final RegisterService registerService;

    /**
     * 상품 주문
     */
    @GetMapping("/order")
    public String createForm(@SessionAttribute(name = "memberId")Long memberId, Model model) {
        // 엔티티 조회
        Member member = memberService.findOne(memberId);   // 로그인한 회원
        List<Register> registers = registerService.findRegisters(); // 모든 아이템과 주문한 회원을 가져오기 위해

        // 엔티티 -> DTO
        MemberDto memberDto = getMemberDto(member);
        // 엔티티 리스트 -> DTO 리스트
        List<RegisterDto> registerDtoList = registers.stream()
                .map(r -> new RegisterDto(r))
                .collect(Collectors.toList());

        model.addAttribute("member", memberDto);    // 스펙에 맞는 DTO 넘김
        model.addAttribute("registers", registerDtoList);
        model.addAttribute("deliveryCodes", deliveryForms());   // 거래(배송) 정보

        return "orders/orderForm";
    }

    @ModelAttribute("deliveryCodes")
    public List<DeliveryForm> deliveryForms() { // 거래(배송) 정보
        List<DeliveryForm> deliveryCodes = new ArrayList<>();
        deliveryCodes.add(new DeliveryForm("DIRECT", "직거래"));
        deliveryCodes.add(new DeliveryForm("DELIVERY", "배송"));

        return deliveryCodes;
    }
    
    @PostMapping("/order")
    // @RequestParam: form - submit 방식에 의해 orderForm 에서 name 파라미터를 받아옴
    public String create(@SessionAttribute(name = "memberId") Long memberId,
                         @RequestParam("registerId") Long registerId,
                         @RequestParam("count") int count,
                         @RequestParam("type") String type) {
        // 컨트롤러에서는 값만 넘기고 서비스 계층의 Tx 안에서 값 변경되게끔
        orderService.order(memberId, registerId, count, type);

        return "redirect:/";
    }

    /**
     * 전체 주문상품 조회 및 검색
     */
    @GetMapping("/orders")  // name 과 orderSearch 가 넘어오면 파라미터를 바인딩시킴
    // @ModelAttribute: model 박스에 자동으로 담긴다고 생각
    public String orderList(@SessionAttribute("memberId") Long memberId,
                            @ModelAttribute("orderSearch") OrderSearch orderSearch,
                            Model model) {  // orderSearch: where 문에 들어갈 조건
        // Where 절에 검색될 조건들을 포함하는 Order 엔티티 리스트
        // List<Order> orders = orderService.findOrders(orderSearch);
        // ToOne 관계 (Member, Deal)은 Fetch join 으로 한방 쿼리로 가져옴
        // orderItems -> item - register (OneToOne 양방향연관관계)에서 N + 1 문제 발생
        // => register 가 영속성 컨텍스트에 존재하지 않으므로 계속 DB 에 쿼리가 나가게 됨
        // => orderItem - item (ManyToOne), item - register (OneToOne) => ToOne 관계 Dto 로 직접 조인하여 해결
        // => in 절에서 order_id로 해당 주문 가져옴
        List<OrderQueryDto> orders = orderRepository.findAllByString_optimization(orderSearch);
        Member member = memberService.findOne(memberId);    // 엔티티 조회

        // 엔티티 -> DTO
        MemberDto memberDto = getMemberDto(member);

        model.addAttribute("member", memberDto);
        model.addAttribute("orders", orders);

        return "orders/orderList";   // 넘어온 파라미터를 바인딩 시킨 후 orderList 화면으로 넘김
    }

    /**
     * 주문 - 거래 완료
     */
    @PostMapping("/orders/{orderId}/complete")  // 거래 완료
    public String completeDeal(@PathVariable("orderId") Long orderId) {
        // 컨트롤러에서는 값만 넘기고 서비스 계층의 Tx 안에서 값 변경되게끔
        orderService.completeDeal(orderId); // 해당 주문의 거래 정보 업데이트

        return "redirect:/orders";
    }

    /**
     * 주문 취소
     */
    @PostMapping("/orders/{orderId}/cancel")   // 주문 취소
    public String cancelOrder(@PathVariable("orderId") Long orderId) {
        // 컨트롤러에서는 값만 넘기고 서비스 계층의 Tx 안에서 값 변경되게끔
        // 해당 주문의 주문 상품 -> 상품 -> 등록 상태까지 변경감지
        orderService.cancelOrder(orderId);

        return "redirect:/orders";
    }

    /**
     * 나의 주문상품 조회
     */
    @GetMapping("/myOrders")
    public String myOrderList(@SessionAttribute("memberId") Long memberId, Model model) {
        // 전체 주문상품 조회와 같은 문제 => 최적화필요
        // List<Order> orders = orderService.findMyOrders(memberId);
        List<OrderQueryDto> orderDtoList = orderRepository.findMyOrders_optimization(memberId);

        model.addAttribute("orders", orderDtoList);

        return "orders/myOrderList";
    }

    /**
     * 나의 주문 - 거래완료
     */
    @PostMapping("/myOrders/{orderId}/complete")
    public String completeMyDeal(@PathVariable("orderId") Long orderId) {
        // 컨트롤러에서는 값만 넘기고 서비스 계층의 Tx 안에서 값 변경되게끔
        orderService.completeDeal(orderId);

        return "redirect:/myOrders";
    }

    /**
     * 나의 주문 - 취소
     */
    @PostMapping("/myOrders/{orderId}/cancel")
    public String cancelMyOrder(@PathVariable("orderId") Long orderId) {
        // 컨트롤러에서는 값만 넘기고 서비스 계층의 Tx 안에서 값 변경되게끔
        orderService.cancelOrder(orderId);

        return "redirect:/myOrders";
    }

    private MemberDto getMemberDto(Member member) { // 엔티티 -> Dto
        return new MemberDto(member.getId(), member.getNickName());
    }
}
