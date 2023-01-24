package mybook.mymarket.controller;


import lombok.RequiredArgsConstructor;
import mybook.mymarket.controller.form.ItemForm;
import mybook.mymarket.controller.form.ItemTypeForm;
import mybook.mymarket.domain.Member;
import mybook.mymarket.domain.Register;
import mybook.mymarket.controller.dto.ItemDto;
import mybook.mymarket.controller.dto.MemberDto;
import mybook.mymarket.controller.dto.RegisterDto;
import mybook.mymarket.domain.item.Item;
import mybook.mymarket.repository.RegisterSearch;
import mybook.mymarket.service.ItemService;
import mybook.mymarket.service.MemberService;
import mybook.mymarket.service.RegisterService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@Controller // 스프링빈에 등록
@RequiredArgsConstructor    // final 키워드를 가진 필드로 생성자를 만들어줌
public class RegisterController {   // Controller 가 Service 갖다씀

    // 등록하기 위해서 회원, 상품 등의 dependency 가 필요함 => 주입
    private final RegisterService registerService;
    private final ItemService itemService;
    private final MemberService memberService;

    /**
     * 상품 등록
     */
    @GetMapping("/register")
    public String createForm(@SessionAttribute(name = "memberId")Long memberId,  Model model) {
        Member member = memberService.findOne(memberId);    // 엔티티 조회 (로그인, 세션)
        MemberDto memberDto = getMemberNameDto(member);     // 엔티티, 회원 이름만 넘김

        model.addAttribute("itemTypeForm", itemTypeForms());    // 상품 분류
        model.addAttribute("member", memberDto);   // 등록할 회원 == 세션(로그인)
        model.addAttribute("form", new ItemForm()); // 등록 상품

        return "registers/createItemForm";
    }

    @ModelAttribute("itemTypeForm")
    public ItemTypeForm[] itemTypeForms() {
        return ItemTypeForm.values();
    }

    @PostMapping("/register")
    public String create(@SessionAttribute(name = "memberId") Long memberId,
                         ItemTypeForm itemTypeForm, ItemForm form) {

        // form -> Dto
        ItemDto itemDto = getItemDto(form);
        registerService.register(memberId, itemDto, itemTypeForm.name(), form.getStockQuantity());

        return "redirect:/";
    }

    /**
     * 상품 조회
     */
    @GetMapping("/registers")
    public String list(@SessionAttribute(name = "memberId") Long memberId,
                       @ModelAttribute("registerSearch") RegisterSearch registerSearch,
                       Model model) {
        // Where 절 검색될 조건들을 포함하는 Register 엔티티 리스트
        List<Register> registers = registerService.findRegistersSearch(registerSearch);

        // 엔티티 리스트 -> DTO 리스트
        List<RegisterDto> registerDtoList = registers.stream()
                .map(r -> new RegisterDto(r))
                .collect(Collectors.toList());

        model.addAttribute("memberId", memberId);
        model.addAttribute("registers", registerDtoList);

        return "registers/itemList";
    }

    /**
     * 등록상품 수정
     */
    @GetMapping("registers/{itemId}/edit")
    public String updateRegisterItemForm(@PathVariable("itemId") Long itemId, Model model) {
        Item item = itemService.findOne(itemId);    // 엔티티 조회

        ItemDto itemDto = getItemDto(item);     //  엔티티 -> DTO
        ItemForm form = createItemForm(itemDto);   // DTO -> Form, 원래 정보 가져오기

        // model 에 key 가 form 인 데이터 form 을 담는다
        model.addAttribute("form", form);   // 이전 등록 form

        return "registers/updateItemForm";
    }

    @PostMapping("registers/{itemId}/edit")
    public String updateItem(@PathVariable("itemId") Long itemId, @ModelAttribute("form")
    @NotNull ItemForm form) {

        registerService.findOneByItem(itemId, form.getStockQuantity()); // 수정 시 외래키(itemId) 이용

        itemService.updateItem(itemId, form.getName(), form.getPrice(), form.getStockQuantity());

        return "redirect:/registers";
    }

    /**
     * 상품 (등록) 취소
     */
    @GetMapping("registers/{registerId}/cancel")
    public String cancelItem(@PathVariable("registerId") Long registerId) {
        registerService.cancelRegister(registerId);

        return "redirect:/registers";
    }

    private MemberDto getMemberNameDto(Member member) { // 엔티티 -> DTO
        return new MemberDto(member.getNickName());
    }

    private static ItemForm createItemForm(ItemDto item) {      // DTO(데이터전송객체) -> Form (화면 종속적)
        ItemForm form = new ItemForm();
        form.setId(item.getId());
        form.setName(item.getName());
        form.setAuthor(item.getAuthor());
        form.setPrice(item.getPrice());
        form.setStockQuantity(item.getStockQuantity());

        return form;
    }

    private ItemDto getItemDto(Item item) { // 엔티티 -> DTO
        // Dto 가 파라미터로 엔티티를 받는 것은 문제가 안됨
        // 왜냐하면 중요하지 않은 곳에서 중요한 엔티티를 의존하기때문에
        return new ItemDto(item);
    }

    private static ItemDto getItemDto(ItemForm form) {  // Form -> DTO

        return new ItemDto(form);
    }
}
