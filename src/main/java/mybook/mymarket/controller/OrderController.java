package mybook.mymarket.controller;


import lombok.RequiredArgsConstructor;
import mybook.mymarket.controller.form.DeliveryForm;
import mybook.mymarket.domain.Member;
import mybook.mymarket.domain.Order;
import mybook.mymarket.domain.Register;
import mybook.mymarket.controller.dto.MemberDto;
import mybook.mymarket.controller.dto.OrderDto;
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
    public List<DeliveryForm> deliveryForms() {
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

//         컨트롤러에서는 값만 넘기고 서비스 계층의 Tx 안에서 값 변경되게끔
        orderService.order(memberId, registerId, count, type);

        return "redirect:/";
    }

    /**
     * 주문 목록 검색, 취소
     */
    @GetMapping("/orders")  // name 과 orderSearch 가 넘어오면 파라미터를 바인딩시킴
    // @ModelAttribute: model 박스에 자동으로 담긴다고 생각
    public String orderList(@SessionAttribute("memberId") Long memberId,
                            @ModelAttribute("orderSearch") OrderSearch orderSearch,
                            Model model) {
        // Where 절에 검색될 조건들을 포함하는 Order 엔티티 리스트
//        List<Order> orders = orderService.findOrders(orderSearch);  // orderSearch: where 문에 들어갈 조건
        List<OrderQueryDto> orders = orderRepository.findAllByString_optimization(orderSearch);
        Member member = memberService.findOne(memberId);    // 엔티티 조회

        // 엔티티 -> DTO
        MemberDto memberDto = getMemberDto(member);

        model.addAttribute("member", memberDto);
        model.addAttribute("orders", orders);

        return "orders/orderList";   // 넘어온 파라미터를 바인딩 시킨 후 orderList 화면으로 넘김
    }

    @GetMapping("/myOrders")
    public String myOrderList(@SessionAttribute("memberId") Long memberId, Model model) {

        Member member = memberService.findOne(memberId);
//        List<Order> orders = orderService.findMyOrders(memberId);
        List<OrderQueryDto> orderDtoList = orderRepository.findMyOrders_optimization(memberId);

        MemberDto memberDto = getMemberDto(member);


        model.addAttribute("members", memberDto);
        model.addAttribute("orders", orderDtoList);

        return "orders/myOrderList";
    }


    @PostMapping("/orders/{orderId}/complete")  // 거래 완료
    public String completeDeal(@PathVariable("orderId") Long orderId) {
        orderService.completeDeal(orderId); // 해당 주문의 거래 정보 업데이트

        return "redirect:/orders";
    }

    @PostMapping("/orders/{orderId}/cancel")   // 주문 취소
    public String cancelOrder(@PathVariable("orderId") Long orderId) {
        orderService.cancelOrder(orderId);

        return "redirect:/orders";
    }

    private MemberDto getMemberDto(Member member) {
        return new MemberDto(member.getId(), member.getNickName());
    }
}
