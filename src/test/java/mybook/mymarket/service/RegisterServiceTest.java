package mybook.mymarket.service;

import mybook.mymarket.controller.dto.ItemDto;
import mybook.mymarket.domain.Address;
import mybook.mymarket.domain.Member;
import mybook.mymarket.domain.Register;
import mybook.mymarket.domain.RegisterStatus;
import mybook.mymarket.domain.item.Item;
import mybook.mymarket.domain.item.Magazine;
import mybook.mymarket.domain.item.Novel;
import mybook.mymarket.repository.ItemRepository;
import mybook.mymarket.repository.MemberRepository;
import mybook.mymarket.repository.RegisterRepository;
import org.assertj.core.api.Assertions;
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
    @Autowired  // 스프링이 스프링 빈에 있는 memberRepository 를 주입해줌
    MemberRepository memberRepository;
    @Autowired
    MemberService memberService;
    @Autowired
    ItemService itemService;
    @Autowired
    ItemRepository itemRepository;
    @Autowired
    RegisterService registerService;
    @Autowired
    RegisterRepository registerRepository;
    @Autowired
    EntityManager em;

    @Test
    public void 상품등록() throws Exception {
        // given
        Member member = createMember("testMember"); // member 세팅
        em.persist(member); // 영속 O

        String type = "Novel";  // 타입
        int price = 10000, stockQuantity = 10;
        Novel novel = createNovel(price, stockQuantity); // item 세팅 아직 영속 X
        ItemDto itemDto = new ItemDto(novel);   // 엔티티 -> Dto
        // when
        Long registerId = registerService.register(member.getId(), itemDto, type, stockQuantity);   // item 영속 O
        System.out.println("registerId = " + registerId);

        // then
        Register register = registerRepository.findOne(registerId); // 검증 대상

        assertEquals("상품 등록시 상태는 REGISTER", RegisterStatus.REGISTER, register.getStatus());
        assertEquals("상품 등록한 회원 이름", member.getNickName(), register.getMember().getNickName());
        assertEquals("등록된 상품 이름", novel.getName(), register.getItem().getName());
        assertEquals("등록된 상품 가격", price, register.getItem().getPrice());
        assertEquals("등록된 상품 재고", stockQuantity, register.getItem().getStockQuantity());
        assertEquals("등록된 상품 분류", type, itemDto.getType().name());
    }

    /**
     * 같은회원이 다른상품으로 등록하면 해당 상품 INSERT
     */
    @Test
    public void 상품등록_같은회원_다른상품() throws Exception {
        // given
        Member member = createMember("testMember"); // member 세팅
        em.persist(member); // 영속 O

        String type = "Novel";  // 타입
        int price = 10000, stockQuantity = 10, updateStockQuantity = 5;
        Novel novel = createNovel(price, stockQuantity); // item 1 세팅 아직 영속 X
        ItemDto itemDto1 = new ItemDto(novel);   // 엔티티 -> Dto
        Long registerId1 = registerService.register(member.getId(), itemDto1, type, stockQuantity);// item 1 영속 O

        // when
        Magazine magazine = createMagazine(price, updateStockQuantity);
        ItemDto itemDto2 = new ItemDto(magazine); // item 2
        Long registerId2 = registerService.register(member.getId(), itemDto2, type, updateStockQuantity);// item 2 영속 -> 같은회원, 다른상품

        // then
        Register register1 = registerRepository.findOne(registerId1); // 검증 대상
        Register register2 = registerRepository.findOne(registerId2);

        assertEquals("상품 등록한 회원 이름", register1.getMember().getNickName(), register2.getMember().getNickName()); // 같은 회원
        assertThat(!register1.getItem().getName().equals(register2.getItem().getName()));    // 다른 상품
        assertThat(!register1.getId().equals(register2.getId()));    // 다른 등록

    }

    /**
     * 다른회원이 같은상품으로 등록하면 해당 상품 INSERT
     */
    @Test
    public void 상품등록_다른회원_같은상품() throws Exception {
        // given
        Member member1 = createMember("testMember1"); // member1 세팅
        em.persist(member1); // 영속 O

        String type = "Novel";  // 타입
        int price = 10000, stockQuantity = 10;
        Novel novel = createNovel(price, stockQuantity); // item 1 세팅 아직 영속 X
        ItemDto itemDto1 = new ItemDto(novel);   // 엔티티 -> Dto
        Long registerId1 = registerService.register(member1.getId(), itemDto1, type, stockQuantity);// item 1 영속 O

        // when
        Member member2 = createMember("testMember2"); // member2 세팅
        em.persist(member2); // 영속 O
        ItemDto itemDto2 = new ItemDto(novel); // 같은상품
        Long registerId2 = registerService.register(member2.getId(), itemDto2, type, stockQuantity);// item 2 영속 -> 같은회원, 같은상품

        // then
        Register register1 = registerRepository.findOne(registerId1); // 검증 대상
        Register register2 = registerRepository.findOne(registerId2);

        assertThat(!register1.getMember().getNickName().equals(register2.getMember().getNickName()));   // 다른회원
        assertEquals("등록된 상품 이름", register1.getItem().getName(), register2.getItem().getName()); // 같은 상품
        assertThat(!register1.getId().equals(register2.getId()));   // 다른 등록
    }

    /**
     * 같은회원이 같은상품으로 등록하면 해당 상품 UPDATE => 변경감지
     */
    @Test
    public void 상품등록_같은회원_같은상품() throws Exception {
        // given
        Member member = createMember("testMember"); // member 세팅
        em.persist(member); // 영속 O

        String type = "Novel";  // 타입
        int price = 10000, stockQuantity = 10, updateStockQuantity = 5;
        Novel novel1 = createNovel(price, stockQuantity); // item 1 세팅 아직 영속 X
        ItemDto itemDto1 = new ItemDto(novel1);   // 엔티티 -> Dto
        registerService.register(member.getId(), itemDto1, type, stockQuantity);   // item 1 영속 O

        // when
        Novel novel2 = createNovel(price, updateStockQuantity);
        ItemDto itemDto2 = new ItemDto(novel2); // item 2
        Long registerId = registerService.register(member.getId(), itemDto2, type, updateStockQuantity);// item 2 영속 -> 같은회원, 같은상품

        // then
        Register register = registerRepository.findOne(registerId); // 검증 대상

        assertEquals("상품 등록시 상태는 REGISTER", RegisterStatus.REGISTER, register.getStatus()); // 등록상태
        assertEquals("상품 등록한 회원 이름", member.getNickName(), register.getMember().getNickName()); // 같은회원
        assertEquals("등록된 상품 이름", novel2.getName(), register.getItem().getName()); // 같은 상품
        assertEquals("등록된 상품 재고", stockQuantity + updateStockQuantity, register.getItem().getStockQuantity());   // 재고 업데이트 -> 15
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

    private static Magazine createMagazine(int price, int stockQuantity) {
        Magazine magazine = new Magazine();
        magazine.setName("testBook");
        magazine.setAuthor("test");
        magazine.setPrice(price);
        magazine.setStockQuantity(stockQuantity);
        magazine.setTheme("test");

        return magazine;
    }

    private static Member createMember(String nickName) {
        Member member = new Member(nickName, "1234", "이정규", new Address("a", "b", "c"));

        return member;
    }

}