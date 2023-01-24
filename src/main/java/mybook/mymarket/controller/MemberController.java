package mybook.mymarket.controller;

import lombok.RequiredArgsConstructor;
import mybook.mymarket.controller.form.MemberForm;
import mybook.mymarket.domain.Address;
import mybook.mymarket.domain.Member;
import mybook.mymarket.controller.dto.MemberDto;
import mybook.mymarket.service.MemberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@Controller // 스프링 빈에 등록
@RequiredArgsConstructor    // final 키워드를 가진 필드(memberService)로 생성자를 만들어줌
public class MemberController {
    private final MemberService memberService;  // 스프링이 스프링빈에 있는 memberRepository 를 주입해줌

    /**
     * 회원가입
     */
    @GetMapping("/join")    //  /join GetMapping: 열기, 접근
    public String createForm(Model model) {
        // Model: Controller 에서 View 로 넘어갈 때 데이터를 Model 에 실어서 넘김
        model.addAttribute("memberForm", new MemberForm());
        // 화면을 이동할 때 loginForm 이라는 빈 껍데기 객체를 가져감
        // 이유: 빈 화면이니까 아무것도 없을 수도 있지만, validation 등을 해줄 수 있기 때문에

        return "members/createMemberForm";   // createMemberForm.html 로 넘어감
    }

    @PostMapping("/join")    // /join PostMapping: 데이터 등록이 목표
    public String create(@Valid MemberForm form, BindingResult result) {
        // MemberForm 대신 Member 를 쓰면 안되는 이유: 화면에 쓰는 form 데이터와 엔티티가
        // 정확하게 일치하지도 않고 가져다가 쓰면 엔티티가 화면 종속적으로 되어 지저분해지므로
        // 유지보수하기도 힘들어지므로 엔티티와 폼을 분리해서 구현하자 (엔티티는 독립적이여야함)
        // @Valid: MemberForm 에 있는 @NotEmpty 를 읽어서 Validation 기능을 수행하게됨
        // BindingResult: @Valid 다음에 BindingResult 가 있으면 오류가 담겨서 실행하게됨
        if(result.hasErrors())    // 담겨진 에러가 있으면
            return "members/createMemberForm";   // 화면까지 에러를 가져가서 뿌리게 됨

        // Form -> Dto
        Member member = createMember(form);     // 객체 생성 및 저장
        memberService.join(member); // 회원 가입

        return "redirect:/"; // 데이터가 저장되고 페이지가 재로딩되면 안좋기때문에 redirect => 처음화면으로
    }

    /**
     * 회원 정보 수정
     */
    @GetMapping("/members/update")
    public String updateMemberForm(@SessionAttribute(name = "memberId") Long memberId, Model model) {
        // 현재 세션 데이터를 이용하여 로그인 회원 가져오기
        Member member = memberService.findOne(memberId);

        MemberDto memberDto = getMemberDto(member);  // 엔티티 -> DTO
        MemberForm form = createMemberForm(memberId, memberDto);   // DTO -> Form, 원래 정보 가져오기
        model.addAttribute("form", form);   // form 객체를 model 에 담아서 넘김

        return "members/updateMemberForm";
    }

    @PostMapping("/members/update")
    public String updateMember(@SessionAttribute(name = "memberId") Long memberId,
                               @ModelAttribute("form") @NotNull MemberForm form) {
        // @ModelAttribute("form") : form 객체에 바인딩하기 위해
        // 영속 엔티티는 JPA 가 dirty checking 을 통해 변경이 일어나면
        // update 쿼리를 날려서 바꿔줌
        // Form -> DTO 바꿔서 객체 날려보자
        MemberDto memberDto = getMemberDto(form);
        memberService.updateMember(memberId, memberDto.getNickName(), memberDto.getPassword(), memberDto.getUserName(), memberDto.getAddress());
        /** 준영속 엔티티
         : 영속성 컨텍스트가 더는 관리하지 않는 엔티티
         => Book 객체는 이미 DB에 한 번 저장되어서 식별자가 존재한다.
         이렇게 임의로 만들어낸 엔티티도 기존 식별자를 가지고 있으면
         준영속 엔티티로 볼 수 있다.
         => 준영속 엔티티를 수정하는 방법 2가지
         1. 변경감지 기능 사용(dirty checking) => 더 나은 방법
         2. 병합(merge) 사용 */

        /** form 객체는 웹 계층에서만 사용한다고 정의내렸기 때문에
         form 을 itemService 서비스 계층으로 넘기기 굉장히 지저분해짐
         해결방안으로 book 객체를 어설프게 만들어서 서비스 계층으로 넘긴 것 => 좋은 설계가 아님
         =>  더 나은 설계는 form 객체가 아닌 필요한 form 객체의 필드값을 서비스 계층으로 넘기자
         => update 할 값이 많은 경우에는 DTO(클래스) 를 만들어서 DTO 의 필드를 넘기자
         */

        /**
         * 1. 변경감지 기능 사용(dirty checking)
         * itemService.updateItem(itemId, form);
         */

        /**
         * 2. 병합(merge) 사용
         * itemService.saveItem(book);
         * ItemService.saveItem() => ItemRepository.save() => null 이 아니므로 em.merge()
         * merge(): DB를 뒤져서 똑같은 식별자를 갖는 엔티티(영속 상태)를 찾음
         => 넘어오는 파라미터로 찾은 엔티티의 값을 세팅함
         => 넘어오는 파라미터는 JPA 가 관리하지 않는 준영속 상태임
         * 주의점: 변경감지 기능을 사용하면 원하는 속성만 선택해서 변경할 수 있지만,
         병합을 사용하면 모든 속성이 변경됨 => 값이 없으면 null 로 없데이트 할 위험도 있다.
         (병합은 모든 필드를 교체한다.) => 선택의 개념이 아님
         */

        return "redirect:/";
    }

    /**
     * 회원 목록 조회
     */
    @GetMapping("/members")
    public String list(Model model) {// model 이라는 객체를 통해서 화면에 전달
        List<Member> members = memberService.findMembers(); // 회원 리스트를 가져와서

        // 엔티티 리스트 -> DTO 리스트
        List<MemberDto> result = members.stream()
                .map(m -> new MemberDto(m))
                .collect(Collectors.toList());

        // model 에 key 가 members 인 데이터 members(회원 리스트)를 담는다
        model.addAttribute("members", result);

        return "members/memberList";   // memberList 로 넘어감
    }

    private static Member createMember(MemberForm form) {   // Form -> 엔티티
        Member member = new Member();
        member.setNickName(form.getNickName()); // 값 세팅
        member.setPassword(form.getPassword());
        member.setUserName(form.getUserName());
        Address address = new Address(form.getCity(), form.getStreet(), form.getZipcode());
        member.setAddress(address);

        return member;
    }

    private static MemberForm createMemberForm(Long memberId, MemberDto memberDto) {    // DTO(데이터전송객체) -> Form (화면 종속적)
        MemberForm form = new MemberForm();     // 객체 생성
        form.setId(memberId);   // 값 세팅
        form.setNickName(memberDto.getNickName());
        form.setPassword(memberDto.getPassword());
        form.setUserName(memberDto.getUserName());
        form.setCity(memberDto.getAddress().getCity());
        form.setStreet(memberDto.getAddress().getStreet());
        form.setZipcode(memberDto.getAddress().getZipcode());

        return form;
    }

    private static MemberDto getMemberDto(Member member) {  // 엔티티 -> DTO
        // Dto 가 파라미터로 엔티티를 받는 것은 문제가 안됨
        // 왜냐하면 중요하지 않은 곳에서 중요한 엔티티를 의존하기때문에
        return new MemberDto(member);
    }

    private static MemberDto getMemberDto(MemberForm form) {    // Form -> DTO

        return new MemberDto(form);
    }
}