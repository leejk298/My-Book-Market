package mybook.mymarket.api;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import mybook.mymarket.controller.dto.RegisterDto;
import mybook.mymarket.controller.form.ItemForm;
import mybook.mymarket.domain.Register;
import mybook.mymarket.repository.RegisterRepository;
import mybook.mymarket.repository.RegisterSearch;
import mybook.mymarket.repository.register.query.RegisterQueryDto;
import mybook.mymarket.repository.register.query.RegisterQueryRepository;
import mybook.mymarket.service.ItemService;
import mybook.mymarket.service.dto.RegisterItemDto;
import mybook.mymarket.service.RegisterService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;


@RestController // @RestController == @Controller + @ResponseBody
// @ResponseBody : data 자체를 바로 Json 이나 XML 로 바로 보내기 위해
@RequiredArgsConstructor // final 키워드를 가진 필드(memberService)로 생성자를 만들어줌
public class RegisterApiController {
    private final RegisterRepository registerRepository;
    private final RegisterQueryRepository registerQueryRepository;
    private final RegisterService registerService;
    private final ItemService itemService;

    /**
     * 상품 등록
     */
    @PostMapping("/api/register/{id}")  // id를 pathVariable 로 가져옴
    public ResponseData<RegisterDto> createRegister(@PathVariable("id") Long memberId,
                                                    @RequestBody @Valid ItemForm form) {
        // 화면 form -> Dto (Service 계층)
        RegisterItemDto itemDto = createRegisterItemDto(form);
        // 등록 수량 <= 0 이면 NotEnoughStockException("need more stock") 발생
        Long registerId = registerService.register(memberId, itemDto);

        // Json 데이터를 보여주기 위한 로직
        Register register = registerRepository.findOne(registerId); // 단권 조회
        RegisterDto registerDto = new RegisterDto(register);    // 엔티티 -> Dto

        // 등록하여 반환된 registerDto 를 Json 형식으로 보여줌
        return new ResponseData<>(registerDto);
    }

    private static RegisterItemDto createRegisterItemDto(ItemForm form) {  // Form -> DTO
        // => 역참조 방지하기 위해 파라미터로 넘김
        return new RegisterItemDto(form.getName(), form.getAuthor(), form.getPrice(), form.getStockQuantity(), form.getItemTypeForm().name(), form.getEtc());
    }

    /**
     * 상품 수정
     * PUT 은 전체 업데이트를 할 때
     * 부분 업데이트를 하려면 PATCH 를 사용하거나 POST 를 사용하는 것이 REST 스타일에 맞다
     */
    @PostMapping("/api/registers/edit/{id}")
    public ResponseData<RegisterDto> updateRegisterItem(@PathVariable("id") Long itemId,
                                                        @RequestBody @Valid ItemForm form) {
        // 로그인을 하면 회원 정보를 세션에 저장하므로 이미 로그인된 상태로 가정
        // 수량은 음수 X => NotEnoughStockException("need more stock") 발생
        /** 커맨드와 쿼리를 분리하자 */
        // 커맨드: update 같은 변경성 메소드는 void 로 끝내거나 id값 정도만 반환함(찾기 위해)
        registerService.findOneByItem(itemId, form.getStockQuantity()); // 수량에 따라 등록 상태 업데이트
        itemService.updateItem(itemId, form.getName(), form.getPrice(), form.getStockQuantity());   // 변경감지

        // 쿼리: 그 후에 별도로 쿼리를 짠다
        // Json 데이터를 보여주기 위한 로직
        Register register = registerRepository.findOneByItem(itemId);
        RegisterDto registerDto = new RegisterDto(register);

        // 등록하여 반환된 registerDto 를 Json 형식으로 보여줌
        return new ResponseData<>(registerDto);
    }

    /**
     * 등록 취소
     */
    @GetMapping("/api/registers/cancel/{id}")
    public ResponseData<RegisterDto> cancelRegisterItem(@PathVariable("id") Long registerId) {
        // 로그인을 하면 회원 정보를 세션에 저장하므로 이미 로그인된 상태로 가정
        registerService.cancelRegister(registerId);
        // 등록을 취소하게 되면 상품 재고가 0, 등록 상태는 CANCEL
        // 등록 상태가 CANCEL 이면 주문 불가

        // Json 데이터를 보여주기 위한 로직
        Register register = registerService.findOne(registerId);
        RegisterDto registerDto = new RegisterDto(register);

        // 등록하여 반환된 registerDto 를 Json 형식으로 보여줌
        return new ResponseData<>(registerDto);
    }


    /**
     * 전체 (등록)상품 조회
     */

    /**
     * v2: 일반 Join - 엔티티
     * 연관 엔티티에 일반 join 을 하게되면 Select 대상의 엔티티는 영속화하여 가져오지만,
     * 조인의 대상은 영속화하여 가져오지 않는다.
     * 연관 엔티티가 검색 조건에 포함되고, 조회의 주체가 검색 엔티티뿐일 때 사용하면 좋다
     * => member, item 에 대한 Proxy 객체 초기화 => Lazy 로딩에 의한 DB 쿼리가 많이 나감
     * => register 1번 - member, item N번(register 조회수만큼) => N + 1 문제 발생
     * => 영속성 컨텍스트에 존재하지 않으면 계속 DB 에 쿼리가 나가므로
     * => 상당히 많은 쿼리가 나감 => 최적화 필요 => Fetch join(v3)
     */
    @GetMapping("/api/v2/registers")
    public Result<List<RegisterDto>> registersV2() {
        List<Register> registers = registerRepository.findAllByRegister();

        List<RegisterDto> result = registers.stream()
                .map(r -> new RegisterDto(r))
                .collect(Collectors.toList());

        return new Result<>(result.size(), result);
    }
    /**
     * 전체 (등록)상품 조회 시 @GetMapping 과 @PostMapping 차이
     * @GetMapping: 전체 상품 조회, @PostMapping: @RequestBody 로 넘긴 조건에 맞는 상품 조회
     * @RequestBody: Json 으로 온 RegisterSearch(data)를 넘김
     * RegisterSearch: where 문에서 검색될 조건들을 만족하는 등록 상품을 조회
     */
    @PostMapping("/api/v2/registers")
    public Result<List<RegisterDto>> registersV2(@RequestBody RegisterSearch registerSearch) {
        List<Register> registers = registerRepository.findAllByString(registerSearch);

        List<RegisterDto> result = registers.stream()
                .map(r -> new RegisterDto(r))
                .collect(Collectors.toList());

        return new Result<>(result.size(), result);
    }

    /**
     * v3: Fetch Join - 엔티티
     * 연관 엔티티에 fetch join 을 하게되면 select 대상의 엔티티 뿐만 아니라 조인의 대상까지 영속화하여 가져온다.
     * 연관 엔티티까지 select 의 대상일 때, N + 1의 문제를 해결하여 가져올 수 있는 좋은 방법이다.
     * v2랑 v3는 로직은 같지만 쿼리가 완전히 다름, Fetch join: 한방쿼리
     * 한 번에 다 끌고와서 엔티티 -> Dto 로 변환하는 단점이 있음 => 최적화: 직접 Dto 이용(v4)
     * register 를 기준으로 X To One 은 무조건 fetch join 을 하고
     * => ToOne 은 데이터 뻥튀기가 안되므로 한 번에 끌고와야 함
     * => Member(다대일), Item(일대일) 이므로 한방쿼리로 나옴
     * => 1 + N => 1로 되어버림
     * => 조인보다 DB 데이터 전송량이 최적화 됨
     */
    @GetMapping("/api/v3/registers")
    public Result<List<RegisterDto>> registersV3() {
        List<Register> registers = registerRepository.findAllWithMemberItem_fetch();

        List<RegisterDto> result = registers.stream()
                .map(r -> new RegisterDto(r))
                .collect(Collectors.toList());

        // Object 타입 {...}으로 반환, Result 라는 껍데기를 씌어서 data 필드의 값은 List 가 나가게됨
        // Object 타입으로 반환하지 않으면 배열타입 [...] 으로 나가게됨 => 확장성, 유연성 X
        return new Result<>(result.size(), result);
    }

    @PostMapping("/api/v3/registers")
    public Result<List<RegisterDto>> registersV3(@RequestBody RegisterSearch registerSearch) {
        List<Register> registers = registerRepository.findAllWithMemberItem_fetch(registerSearch);

        List<RegisterDto> result = registers.stream()
                .map(r -> new RegisterDto(r))
                .collect(Collectors.toList());

        // Object 타입 {...}으로 반환, Result 라는 껍데기를 씌어서 data 필드의 값은 List 가 나가게됨
        // Object 타입으로 반환하지 않으면 배열타입 [...] 으로 나가게됨 => 확장성, 유연성 X
        return new Result<>(result.size(), result);
    }

    /**
     * v4: 일반 Join - Dto
     * : ToOne 관계(M, D)들 조회 => findRegisters()
     * tradeoff => Fetch 조인(V3)보다 쿼리를 직접 작성하는 양도 많지만,
     * 장점은 Fetch 조인보다 확실히 데이터를 select 한 양이 줄어듦
     */
    @GetMapping("/api/v4/registers")
    public Result<List<RegisterQueryDto>> registersV4() {
        List<RegisterQueryDto> allByDto = registerQueryRepository.findAllByDto();

        return new Result<>(allByDto.size(), allByDto);
    }

    @PostMapping("/api/v4/registers")
    public Result<List<RegisterQueryDto>> registersV4(@RequestBody RegisterSearch registerSearch) {
        List<RegisterQueryDto> allByDto = registerQueryRepository.findAllByDto_search(registerSearch);

        return new Result<>(allByDto.size(), allByDto);
    }

    /**
     * v3와 v4의 차이점
     * Fetch join(v3)는 XToOne 이 아닌 컬렉션 조회 시 hibernate.default_batch_fetch_size , @BatchSize 같이
     * 코드를 거의 수정하지 않고, 옵션만 약간 변경해서, 다양한 성능 최적화를 시도할 수 있다.
     * 반면에 DTO 를 직접 조회하는 방식(v4)은 성능을 최적화하거나 성능최적화 방식을 변경할 때 많은 코드를 변경해야 한다.
     * V3 (Fetch join)의 경우 join 은 V4와 성능이 같지만, Select 절에서 데이터를 많이 긁어오므로
     * DB 에서 많이 퍼올리게됨 => 네트워크 리소스 잡아먹게 됨
     * 반면 V4에서는 Select 절이 확 줄어듦 => 왜냐하면 직접 쿼리를 짰기때문에 필요한 것만 가져옴
     * 하지만 V3와 V4의 우열을 가릴 수 없음 => V3는 내가 원하는 테이블을 조인하여 값을 가져온 것이고
     * V4는 실제 SQL 짜듯이 필요한 것 전부 가져온 것 => 더이상 재사용성 X => 딱 이거일 때만 사용해야함
     * 하지만 V3는 재사용성이 높음 => Fetch join 으로 가져오고 그걸 다른 Dto 로 변환해서 사용이 가능함
     * V4는 성능 최적화지만 엔티티를 Dto 로 바꿔서 가져온 게 아니고 직접 Dto 로 조회한 것이므로 값 변경이 안되고
     * V3는 V4보다 성능은 떨어지지만 (차이는 미비함) 재사용성이 높고, 코드 상 V4는 지저분해지게 됨
     * 또한 API 스펙에 맞춘 코드가 repository 에 들어가게 됨 => repository 사용은 엔티티에 대한 객체 그래프 탐색이여야함
     * => repository 에서 Query 용 디렉토리를 따로 생성하여 관리 => RegisterQueryRepository
     */

    /**
     * 쿼리 방식 선택 권장 순서 (V3 <-> V4)
     1. 우선 엔티티를 DTO 로 변환하는 방법을 선택(V2) -> 코드 유지보수성 좋음
     2. 필요하면 Fetch join 으로 성능을 최적화한다(V3) -> 대부분의 성능이슈 해결가능, 90 %
     => Register 조회의 경우에는 Fetch join 으로 모든 ToOne 관계를 한 번에 갖고 오므로 N + 1 문제 해결
     3. 그래도 안되면 DTO 로 직접 조회하는 방법을 사용(V4) - Fetch join 사용 불가능
     4. 최후의 방법은 JPA 가 제공하는 네이티브 SQL 이나 SpringJDBCTemplate 을 사용해서 SQL 직접 사용
     */

    /**
     * 나의 (등록)상품 조회
     */
    @GetMapping("/api/v2/myRegisters/{id}")
    public Result<List<RegisterDto>> myRegistersV2(@PathVariable("id") Long memberId) {
        List<Register> myRegisters = registerRepository.findMyRegisters(memberId);

        List<RegisterDto> result = myRegisters.stream()
                .map(r -> new RegisterDto(r))
                .collect(Collectors.toList());

        return new Result<>(result.size(), result);
    }

    @GetMapping("/api/v3/myRegisters/{id}")
    public Result<List<RegisterDto>> myRegistersV3(@PathVariable("id") Long memberId) {
        List<Register> myRegisters = registerRepository.findMyRegisters_fetch(memberId);

        List<RegisterDto> result = myRegisters.stream()
                .map(r -> new RegisterDto(r))
                .collect(Collectors.toList());

        return new Result<>(result.size(), result);
    }

    @GetMapping("/api/v4/myRegisters/{id}")
    public Result<List<RegisterQueryDto>> myRegistersV4(@PathVariable("id") Long memberId) {
        List<RegisterQueryDto> myAllByDto = registerQueryRepository.findMyAllByDto(memberId);

        return new Result<>(myAllByDto.size(), myAllByDto);
    }

    @Data
    @AllArgsConstructor
    static class ResponseData<T> {
        private T data;
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private int count;
        private T data;
    }
}
