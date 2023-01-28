package mybook.mymarket.controller;


import lombok.RequiredArgsConstructor;
import mybook.mymarket.controller.form.ItemForm;
import mybook.mymarket.controller.form.ItemTypeForm;
import mybook.mymarket.domain.Register;
import mybook.mymarket.controller.dto.RegisterDto;
import mybook.mymarket.domain.item.Item;
import mybook.mymarket.repository.RegisterSearch;
import mybook.mymarket.service.ItemService;
import mybook.mymarket.service.RegisterItemDto;
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

    /**
     * 상품 등록
     */
    @GetMapping("/register")
    public String createForm(Model model) {
        // Model: Controller 에서 View 로 넘어갈 때 데이터를 Model 에 실어서 넘김
        model.addAttribute("itemTypeForm", itemTypeForms());    // 상품 분류
        model.addAttribute("form", new ItemForm()); // 등록 상품
        // 화면을 이동할 때 loginForm 이라는 빈 껍데기 객체를 가져감
        // 이유: 빈 화면이니까 아무것도 없을 수도 있지만, validation 등을 해줄 수 있기 때문에

        return "registers/createItemForm";
    }

    @ModelAttribute("itemTypeForm")
    public ItemTypeForm[] itemTypeForms() { // 라디오버튼
        return ItemTypeForm.values();   // 값 가져옴
    }

    @PostMapping("/register")
    public String create(@SessionAttribute(name = "memberId") Long memberId, ItemForm form) {
        // 파라미터로 넘기면 아무래도 재사용성은 좋음
        // DTO 를 넘기면 편하지만, 해당 엔티티가 DTO 에 의존하게 됨
        // => Service 계층에서 Controller 계층을 참조하게됨 => service 게층용 Dto 생성
        // 그래도 파라미터가 너무 많으면 dto 를 만드는 것을 고려하는 것이 좋다
        RegisterItemDto registerItemDto = createRegisterItemDto(form);  // form -> service 계층 Dto
        registerService.register(memberId, registerItemDto);    // Dto 넘겨줌

        return "redirect:/";
    }

    /**
     * 전체 (등록)상품 조회
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
        // 엔티티 조회
        Item item = itemService.findOne(itemId);
        // 엔티티 -> Form, 원래 정보 가져오기
        ItemForm form = createItemForm(item);
        // model 에 key 가 form 인 데이터 form 을 담는다
        model.addAttribute("form", form);   // 이전 등록 form

        return "registers/updateItemForm";
    }

    @PostMapping("registers/{itemId}/edit")
    public String updateItem(@PathVariable("itemId") Long itemId, @ModelAttribute("form")
    @NotNull ItemForm form) {
        // 수량에 따라 등록 상태 업데이트 필요
        registerService.findOneByItem(itemId, form.getStockQuantity()); // 수정 시 외래키(item_id) 이용
        // 컨트롤러에서는 값만 넘기고 서비스 계층의 Tx 안에서 값 변경되게끔 => 변경감지
        itemService.updateItem(itemId, form.getName(), form.getPrice(), form.getStockQuantity());

        return "redirect:/registers";
    }

    /**
     * 상품 (등록) 취소
     */
    @GetMapping("registers/{registerId}/cancel")
    public String cancelItem(@PathVariable("registerId") Long registerId) {
        // 컨트롤러에서는 값만 넘김 => 서비스 계층에서 데이터 변경 => 상품 재고 업데이트(변경감지)
        registerService.cancelRegister(registerId);

        return "redirect:/registers";
    }

    /**
     * 나의 (등록)상품 조회
     */
    @GetMapping("/myRegisters")
    public String myList(@SessionAttribute(name = "memberId") Long memberId, Model model) {
        // 엔티티 List
        List<Register> registers = registerService.findMyRegisters(memberId);
        // 엔티티 List -> Dto List
        List<RegisterDto> registerDtoList = registers.stream()
                .map(r -> new RegisterDto(r))
                .collect(Collectors.toList());

        model.addAttribute("memberId", memberId);
        model.addAttribute("registers", registerDtoList);

        return "registers/myItemList";
    }

    /**
     * 나의 (등록)상품 수정
     */
    @GetMapping("myRegisters/{itemId}/edit")
    public String updateMyRegisterItemForm(@PathVariable("itemId") Long itemId, Model model) {
        // 엔티티 조회
        Item item = itemService.findOne(itemId);
        // 엔티티 -> Form, 원래 정보 가져오기
        ItemForm form = createItemForm(item);
        // model 에 key 가 form 인 데이터 form 을 담는다
        model.addAttribute("form", form);   // 이전 등록 form

        return "registers/updateItemForm";
    }

    @PostMapping("myRegisters/{itemId}/edit")
    public String updateMyItem(@PathVariable("itemId") Long itemId, @ModelAttribute("form")
    @NotNull ItemForm form) {
        // 수량에 따라 등록 상태 업데이트 필요
        registerService.findOneByItem(itemId, form.getStockQuantity()); // 수정 시 외래키(itemId) 이용
        // 컨트롤러에서는 값만 넘기고 서비스 계층의 Tx 안에서 값 변경되게끔 => 변경감지
        itemService.updateItem(itemId, form.getName(), form.getPrice(), form.getStockQuantity());

        return "redirect:/myRegisters";
    }

    /**
     * 나의 (등록)상품 취소
     */
    @GetMapping("myRegisters/{registerId}/cancel")
    public String cancelMyItem(@PathVariable("registerId") Long registerId) {
        // 컨트롤러에서는 값만 넘김 => 서비스 계층에서 데이터 변경 => 상품 재고 업데이트(변경감지)
        registerService.cancelRegister(registerId);

        return "redirect:/myRegisters";
    }

    private static ItemForm createItemForm(Item item) {    // 엔티티 -> Form (화면 종속적)
        ItemForm form = new ItemForm();
        form.setName(item.getName());
        form.setAuthor(item.getAuthor());
        form.setPrice(item.getPrice());
        form.setStockQuantity(item.getStockQuantity());

        return form;
    }

    private static RegisterItemDto createRegisterItemDto(ItemForm form) {  // Form -> DTO
        // Controller 계층이 Service 계층을 참조하는 것은 문제없음
        return new RegisterItemDto(form);   // Service 계층 Dto
    }
}
