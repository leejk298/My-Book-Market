package mybook.mymarket.service;

import mybook.mymarket.domain.Address;
import mybook.mymarket.domain.Member;
import mybook.mymarket.domain.Register;
import mybook.mymarket.domain.RegisterStatus;
import mybook.mymarket.exception.NotEnoughStockException;
import mybook.mymarket.repository.RegisterRepository;
import mybook.mymarket.service.dto.RegisterItemDto;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;


@SpringBootTest     // 스프링 위에서 테스트
@RunWith(SpringRunner.class)    // 스프링 컨테이너 안에서 실행
@Transactional      // 데이터 변겅이 일어나므로, 롤백시키기 위해
public class RegisterServiceTest {
    @Autowired
    ItemService itemService;
    @Autowired
    RegisterService registerService;
    @Autowired
    RegisterRepository registerRepository;
    @Autowired
    EntityManager em;

    /**
     * 상품 등록
     */
    @Test
    public void 상품등록() throws Exception {
        // given: 회원, 상품 세팅
        // 등록회원
        Member member = createMember("testMember"); // member 세팅
        em.persist(member); // 영속 O

        // 상품 세팅
        String type = "Novel";  // 타입: 라디오 버튼으로 받아옴
        String itemName = "testItem", author = "test", etc = "test";
        int price = 10000, stockQuantity = 10;
        // 화면 종속적인 Form 데이터를 입력받아서 서비스계층Dto 인 RegisterItemDto 객체생성
        RegisterItemDto itemDto = new RegisterItemDto(itemName, author, price, stockQuantity, type, etc);

        // when: 상품 등록
        Long registerId = registerService.register(member.getId(), itemDto);   // item 영속 O

        // then
        Register register = registerRepository.findOne(registerId); // 검증 대상

        assertEquals("상품 등록 시 상태는 REGISTER", RegisterStatus.REGISTER, register.getStatus());
        assertEquals("상품 등록한 회원 이름", member.getNickName(), register.getMember().getNickName());
        assertEquals("등록된 상품 이름", itemName, register.getItem().getName());
        assertEquals("등록된 상품 가격", price, register.getItem().getPrice());
        assertEquals("등록된 상품 재고", stockQuantity, register.getItem().getStockQuantity());
    }

    /**
     * 같은회원이 다른상품으로 등록하면 해당 상품 INSERT
     */
    @Test
    public void 상품등록_같은회원_다른상품() throws Exception {
        // given: 회원, 상품 세팅
        // 등록회원
        Member member = createMember("testMember"); // member 세팅
        em.persist(member); // 영속 O

        // 상품 세팅
        String type1 = "Novel";  // 타입: 라디오 버튼으로 받아옴
        String itemName1 = "testItem1", author1 = "test1", etc1 = "test1";
        int price1 = 10000, stockQuantity1 = 10;
        // 화면 종속적인 Form 데이터를 입력받아서 서비스계층Dto 인 RegisterItemDto 객체생성
        RegisterItemDto itemDto1 = new RegisterItemDto(itemName1, author1, price1, stockQuantity1, type1, etc1);
        Long registerId1 = registerService.register(member.getId(), itemDto1);// item 1 영속 O

        // when: 상품 등록2
        String type2 = "Magazine";  // 타입: 라디오 버튼으로 받아옴
        String itemName2 = "testItem2", author2 = "test2", etc2 = "test2";
        int price2 = 10000, stockQuantity2 = 10;
        RegisterItemDto itemDto2 = new RegisterItemDto(itemName2, author2, price2, stockQuantity2, type2, etc2);
        Long registerId2 = registerService.register(member.getId(), itemDto2);// item 2 영속 -> 같은회원, 다른상품

        // then
        Register register1 = registerRepository.findOne(registerId1); // 검증 대상
        Register register2 = registerRepository.findOne(registerId2);

        assertEquals("상품 등록한 회원 이름", register1.getMember().getNickName(), register2.getMember().getNickName()); // 같은 회원
        assertThat(!(register1.getItem().getName().equals(register2.getItem().getName())));    // 다른 상품
        assertThat(!(register1.getId().equals(register2.getId())));    // 다른 등록
    }

    /**
     * 같은회원이 같은상품으로 등록하면 해당 상품 UPDATE => 변경감지
     */
    @Test
    public void 상품등록_같은회원_같은상품() throws Exception {
        // given: 회원, 상품 세팅
        // 등록회원
        Member member = createMember("testMember"); // member 세팅
        em.persist(member); // 영속 O

        // 상품 세팅 1
        String type1 = "Novel";  // 타입: 라디오 버튼으로 받아옴
        String itemName1 = "testItem1", author1 = "test1", etc1 = "test1";
        int price1 = 10000, stockQuantity1 = 10;
        // 화면 종속적인 Form 데이터를 입력받아서 서비스계층Dto 인 RegisterItemDto 객체생성
        RegisterItemDto itemDto1 = new RegisterItemDto(itemName1, author1, price1, stockQuantity1, type1, etc1);
        registerService.register(member.getId(), itemDto1);// item 1 영속 O

        // when: 같은 상품 등록
        String type2 = "Novel";  // 타입: 라디오 버튼으로 받아옴
        String itemName2 = "testItem1", author2 = "test1", etc2 = "test1";
        int price2 = 10000, stockQuantity2 = 40;
        // 화면 종속적인 Form 데이터를 입력받아서 서비스계층Dto 인 RegisterItemDto 객체생성
        RegisterItemDto itemDto2 = new RegisterItemDto(itemName2, author2, price2, stockQuantity2, type2, etc2);
        Long registerId = registerService.register(member.getId(), itemDto2);// item 2 영속 O

        // then
        Register register = registerRepository.findOne(registerId); // 검증 대상

        assertEquals("상품 등록 시 상태는 REGISTER", RegisterStatus.REGISTER, register.getStatus()); // 등록상태
        assertEquals("상품 등록한 회원 이름", member.getNickName(), register.getMember().getNickName()); // 같은회원
        assertEquals("등록된 상품 이름", itemName1, register.getItem().getName()); // 같은 상품
        assertEquals("등록된 상품 재고", stockQuantity1 + stockQuantity2, register.getItem().getStockQuantity());   // 재고 업데이트 -> 50
    }

    /**
     * 상품 취소
     */
    @Test
    public void 상품취소() throws Exception {
        // given: 회원, 상품 세팅
        // 등록회원
        Member member = createMember("testMember"); // member 세팅
        em.persist(member); // 영속 O

        // 상품 세팅 및 등록
        String type = "Novel";  // 타입: 라디오 버튼으로 받아옴
        String itemName = "testItem", author = "test", etc = "test";
        int price = 10000, stockQuantity = 10;
        // 화면 종속적인 Form 데이터를 입력받아서 서비스계층Dto 인 RegisterItemDto 객체생성
        RegisterItemDto itemDto = new RegisterItemDto(itemName, author, price, stockQuantity, type, etc);
        Long registerId = registerService.register(member.getId(), itemDto);   // item 영속 O

        // when: 상품 취소
        registerService.cancelRegister(registerId); // 상품 취소 => 상품 재고, 등록 상태 변경
        Register register = registerService.findOne(registerId);    // 검증 대상

        // then
        assertEquals("상품 취소 시 상태는 CANCEL", RegisterStatus.CANCEL, register.getStatus());
        assertEquals("상품 취소 시 재고는 0", 0, register.getItem().getStockQuantity());
        assertEquals("상품 등록한 회원 이름", member.getNickName(), register.getMember().getNickName()); // 같은회원
        assertEquals("등록된 상품 이름", itemName, register.getItem().getName()); // 같은 상품
    }

    /**
     * 상품 수정
     */
    @Test
    public void 상품수정() throws Exception {
        // given: 회원, 상품 세팅
        // 등록회원
        Member member = createMember("testMember"); // member 세팅
        em.persist(member); // 영속 O

        // 상품 세팅 및 등록
        String type = "Novel";  // 타입: 라디오 버튼으로 받아옴
        String itemName = "testItem", author = "test", etc = "test";
        int price = 10000, stockQuantity = 10;
        // 화면 종속적인 Form 데이터를 입력받아서 서비스계층Dto 인 RegisterItemDto 객체생성
        RegisterItemDto itemDto = new RegisterItemDto(itemName, author, price, stockQuantity, type, etc);
        Long registerId = registerService.register(member.getId(), itemDto);   // item 영속 O
        Register register = registerService.findOne(registerId);

        // when: 상품 수정
        String updateName = "testNovel";
        int updatePrice = 5000, updateStockQuantity = 0;
        // 재고 0 => 등록 상태 업데이트
        registerService.findOneByItem(register.getItem().getId(), updateStockQuantity);
        // 상품 수정 => 변경감지
        itemService.updateItem(register.getItem().getId(), updateName, updatePrice, updateStockQuantity);

        // then
        assertEquals("상품 수정 시 이름 업데이트", register.getItem().getName(), updateName);
        assertEquals("상품 수정 시 가격 업데이트", register.getItem().getPrice(), updatePrice);
        assertEquals("상품 수정 시 재고 업데이트", register.getItem().getStockQuantity(),updateStockQuantity);
        assertEquals("재고가 0이면 등록 상태는 CANCEL", RegisterStatus.CANCEL, register.getStatus());
    }

    @Test(expected = NotEnoughStockException.class)
    public void 상품수정_재고예외() throws Exception {
        // given: 회원, 상품 세팅
        // 등록회원
        Member member = createMember("testMember"); // member 세팅
        em.persist(member); // 영속 O

        // 상품 세팅 및 등록
        String type = "Novel";  // 타입: 라디오 버튼으로 받아옴
        String itemName = "testItem", author = "test", etc = "test";
        int price = 10000, stockQuantity = 10;
        // 화면 종속적인 Form 데이터를 입력받아서 서비스계층Dto 인 RegisterItemDto 객체생성
        RegisterItemDto itemDto = new RegisterItemDto(itemName, author, price, stockQuantity, type, etc);
        Long registerId = registerService.register(member.getId(), itemDto);   // item 영속 O
        Register register = registerService.findOne(registerId);

        // when: 상품 수정
        String updateName = "testNovel";
        int updatePrice = 5000, updateStockQuantity = -1;   // 수량은 음수 X => 예외 발생
        registerService.findOneByItem(register.getItem().getId(), updateStockQuantity);  // "need more stock"
        itemService.updateItem(register.getItem().getId(), updateName, updatePrice, updateStockQuantity);

        // then
        Assert.fail("재고는 음수가 안되므로 예외가 발생해야 한다.");  // 여기로 오면 잘못 작성한 테스트 케이스
    }

    private static Member createMember(String nickName) {
        Member member = new Member(nickName, "1234", "이정규", new Address("a", "b", "c"));

        return member;
    }
}