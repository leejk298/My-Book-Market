package mybook.mymarket.api;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import mybook.mymarket.controller.dto.MemberDto;
import mybook.mymarket.domain.Address;
import mybook.mymarket.domain.Member;
import mybook.mymarket.service.MemberService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController // @RestController == @Controller + @ResponseBody
// @ResponseBody: data 자체를 바로 Json 이나 XML 로 바로 보내기위헤
@RequiredArgsConstructor    // final 키워드를 가진 필드(memberService)로 생성자를 만들어줌
public class MemberApiController {
    private final MemberService memberService;  // Controller 가 Service 갖다씀

    /**
     * API 는 항상 요청이 들어오고 나가는 것으로
     * 전부 다 절대 엔티티 사용 X => 엔티티 말고 Dto 사용
     */
    // @Valid: Member 에서 javax 와 관련된 것 validation 되게 해줌
    // @RequestBody: Json 으로 온 body(data)를 Member 로 그대로 매핑해줌
    // 1. 엔티티에 표현계층을 위한 검증로직을 사용하므로 결합도가 높아져서 안좋은 설계
    // 2. 엔티티의 필드 값을 바꾸면(nickName -> idName) API 스펙 자체가 바뀌게 되므로
    // 또한 엔티티는 여러 곳에서 쓰이므로 바뀔 확률도 높음
    // 그러므로 엔티티가 바뀔 때마다 API 스펙 자체가 바뀌는 것이 문제
    // => 해당 API 를 신뢰할 수가 없음, 또한 엔티티 자체를 외부에 유출하면 안됨
    // 결론: API 요청스펙에 맞춰서 별도의 Dto(Data transfer object)를 파라미터로 받자
    // 엔티티는 절대 사용하면 안됨 => 사이드 이펙트가 일어나서 큰 장애가 발생함

    /**
     * 회원 등록
     */
    @PostMapping("/api/joinMembers") // 등록이므로 @PostMapping
    public CreateMemberResponse saveMember(@RequestBody @Valid MemberDto memberDto) {
        // 별도의 Dto(Data transfer object)를 파라미터
        // @RequestBody: 요청이 온 data 를 RequestBody 로 CreateMemberRequest 에 바인딩
        // 장점: (nickName -> idName)으로 바꿔도 컴파일 에러로 찾을 수 있어서 API 스펙이 바뀌질 않음
        // Member 엔티티는 파라미터로 무슨 값이 들어올 지 모르는데
        // Dto 로 받으면 스펙을 보고 무슨 값이 들어오는 지 알 수 있고, validation 도 자유롭게 가능함
        // 표현 계층과 엔티티 분리하게 됨 => 유지보수, 관리가 편함
        Member member = createMember(memberDto);

        Long id = memberService.join(member);   // 회원 등록

        return new CreateMemberResponse(id);    // 등록하여 반환된 id를 Json 형식으로 보여줌
    }

    /**
     * 회원 수정
     * PUT 은 전체 업데이트를 할 때
     * 부분 업데이트를 하려면 PATCH 를 사용하거나 POST 를 사용하는 것이 REST 스타일에 맞다
     */
    @PatchMapping("/api/members/{id}")  // id를 pathVariable 로 가져옴
    // 등록(CreateMemberResponse)를 그대로 써도 될 것 같지만, 등록과 수정은 API 스펙이 거의 다 다름
    // 수정은 되게 제한적으므로 별도의 수정(UpdateMemberResponse)을 쓰자
    public UpdateMemberResponse updateMember(@PathVariable("id") Long id,
                                             @RequestBody @Valid MemberDto memberDto) {
        // PathVariable 로 id 와 memberDto 의 name 과 address 가 넘어옴
        /** 커맨드와 쿼리를 분리하자 */
        // 커맨드: update 같은 변경성 메소드는 void 로 끝내거나 id값 정도만 반환함(찾기 위해)
        memberService.updateMember(id, memberDto.getNickName(), memberDto.getPassword(), memberDto.getUserName(), memberDto.getAddress());
        // 쿼리: 그 후에 별도로 쿼리를 짠다
        Member findMember = memberService.findOne(id);
        // 등록하여 반환된 필드 값들을 Json 형식으로 보여줌
        // @AllArgsConstructor 를 썼기때문에 모든 파라미터를 담는 생성자 필요
        return new UpdateMemberResponse(findMember.getId(), findMember.getNickName(), findMember.getUserName(), findMember.getAddress());
    }

    /**
     * 회원 조회
     */
    @GetMapping("/api/members") // get 메소드는 body 부분 필요 x
    // API 스펙이 곧 Dto 랑 코드가 1대1로 매칭됨 => 유지보수 편함
    public Result members() {
        // 엔티티 조회
        List<Member> findMembers = memberService.findMembers();

        // 엔티티 List -> Dto List
        List<MemberDto> MemberListDto = findMembers.stream()
                // 엔티티(m)를 Dto 에 넣어서 매핑
                .map(m -> new MemberDto(m))
                // 매핑한 것을 List 로 변환
                .collect(Collectors.toList());

        // Object 타입 {...}으로 반환, Result 라는 껍데기를 씌어서 data 필드의 값은 List 가 나가게됨
        // Object 타입으로 반환하지 않으면 배열타입 [...] 으로 나가게됨 => 확장성, 유연성 X
        return new Result(MemberListDto.size(), MemberListDto);
    }


    // 안에서만 사용할 것이므로 내부클래스(Inner Class)로
    @Data
    @AllArgsConstructor
    static class CreateMemberResponse { // 응답 값
        private Long id;    // 회원이 등록(saveMember)되면 id값 리턴되게
    }

    private static Member createMember(MemberDto memberDto) {
        Member member = new Member();   // 엔티티 객체 생성
        // 파라미터랑 엔티티를 컨트롤러에서 매핑시켜줌
        member.setNickName(memberDto.getNickName());  // 값 세팅
        member.setPassword(memberDto.getPassword());
        member.setUserName(memberDto.getUserName());
        member.setAddress(memberDto.getAddress());

        return member;
    }

    @Data
    @AllArgsConstructor // 필드 값을 전부 포함하는 생성자를 만들어줌
    static class UpdateMemberResponse {
        private Long id;
        private String nickName;
        private String userName;
        private Address address;
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {    // 한 번 감싸서 반환 => Object
        private int count; // collect.size(): 몇 개가 있는 지
        private T data;     // 제네릭 타입
    }
}