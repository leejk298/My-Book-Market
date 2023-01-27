package mybook.mymarket.service;

import lombok.RequiredArgsConstructor;
import mybook.mymarket.domain.Member;
import mybook.mymarket.domain.Register;
import mybook.mymarket.domain.RegisterStatus;
import mybook.mymarket.controller.dto.ItemDto;
import mybook.mymarket.controller.dto.ItemTypeDto;
import mybook.mymarket.domain.item.Item;
import mybook.mymarket.domain.item.Magazine;
import mybook.mymarket.domain.item.Novel;
import mybook.mymarket.domain.item.Reference;
import mybook.mymarket.exception.NotEnoughStockException;
import mybook.mymarket.repository.ItemRepository;
import mybook.mymarket.repository.MemberRepository;
import mybook.mymarket.repository.RegisterRepository;
import mybook.mymarket.repository.RegisterSearch;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service    // 스프링빈에 등록
@Transactional(readOnly = true) // 읽기전용: 리소스 낭비 X, default: false
@RequiredArgsConstructor    // final 키워드를 가진 필드(itemRepository, registrationRepository, memberRepository)로 생성자를 만들어줌
public class RegisterService {

    private final ItemRepository itemRepository;
    private final RegisterRepository registerRepository;
    private final MemberRepository memberRepository;

    public List<Register> findRegisters() {
        return registerRepository.findAllByRegister();
    }

    public List<Register> findRegistersSearch(RegisterSearch registerSearch) {
        return registerRepository.findAllByString(registerSearch);
    }

    @Transactional
    public void findOneByItem(Long id, int count) {
        Register register = registerRepository.findOneByItem(id);   // 등록상품으로 해당 등록 가져오기

        // 수량에 따라 등록 상태 업데이트
        if (count == 0) {   // 수량이 0이면 CANCEL, ? -> 0
            register.setStatus(RegisterStatus.CANCEL);
        } else if (count > 0) { // 수량이 0보다 크면 REGISTER, 0 -> ? 포함
            register.setStatus(RegisterStatus.REGISTER);
        } else {    // 0보다 작으면 오류
            throw new NotEnoughStockException("need more stock");
        }
    }

    public Register findOne(Long id) {
        return registerRepository.findOne(id);
    }

    /**
     * 등록 => 데이터 변경 필요 => Transactional
     * - 등록상품이 겹치는지 유무 -
     */
    @Transactional
    public Long register(Long memberId, ItemDto itemDto, String type, int count) {
        // 등록 수량 <= 0 이면 NotEnoughStockException("need more stock") 발생
        if (count <= 0) {
            throw new NotEnoughStockException("need more stock");
        }

        // 엔티티 조회
        Member member = memberRepository.findOne(memberId); // 로그인 id

        // Novel
        if (type.equals("Novel")) {
            itemDto.setType(ItemTypeDto.Novel);   // 상품 분류 세팅
            Novel novel = createNovel(itemDto);    // 객체 생성 및 세팅
            // 해당 id로 같은 상품을 등록했는지 체크
            Optional<Item> findItem = itemRepository.findByMemberAndItem(memberId, novel.getName());
            if (findItem.isPresent()) {  // 같은 상품이 등록되어있을 때 => update
                findItem.get().addStock(count); // 변경감지
                return findItem.get().getRegister().getId();    // 원래 있던 등록 id 리턴
             } else {    // 다른 상품인 경우 (null) => insert
                Register register = Register.createRegister(member, novel);  // 등록 => 생성 메소드 이용 (static)
                registerRepository.save(register);  // 등록 저장 -> 상품 저장 (Cascade)
                return register.getId();    // 등록 id 값 반환
            }
        }
        // Magazine
        else if (type.equals("Magazine")) {
            itemDto.setType(ItemTypeDto.Magazine);
            Magazine magazine = createMagazine(itemDto);
            Optional<Item> findItem = itemRepository.findByMemberAndItem(memberId, magazine.getName());
            if (findItem.isPresent()) {
                findItem.get().addStock(count);
                return findItem.get().getRegister().getId();
            } else {
                Register register = Register.createRegister(member, magazine);
                registerRepository.save(register);
                return register.getId();
            }
        }
        // Reference
        else {
            itemDto.setType(ItemTypeDto.Reference);
            Reference reference = createReference(itemDto);
            Optional<Item> findItem = itemRepository.findByMemberAndItem(memberId, reference.getName());
            if (findItem.isPresent()) {  // 같은 상품이 등록되어있을 때 => update
                findItem.get().addStock(count); // 변경감지
                return findItem.get().getRegister().getId();    // 원래 있던 등록 id 리턴
            } else {    // 다른 상품인 경우 (null) => insert
                Register register = Register.createRegister(member, reference);  // 등록 => 생성 메소드 이용 (static)
                registerRepository.save(register);  // 등록 저장
                return register.getId();    // 등록 id 값 반환
            }
        }
    }

    /**
     * 등록 취소 => 데이터 변경 필요 => Transactional
     */
    @Transactional
    public void cancelRegister(Long registrationId) {
        // 엔티티 조회
        Register register = registerRepository.findOne(registrationId); // 해당 등록 가져와서

        register.cancel();  // 등록 취소 -> 상품 재고 업데이트
    }

    private static Novel createNovel(ItemDto itemDto) {
        Novel novel = new Novel();

        novel.setName(itemDto.getName());
        novel.setAuthor(itemDto.getAuthor());
        novel.setPrice(itemDto.getPrice());
        novel.setStockQuantity(itemDto.getStockQuantity());
        novel.setGenre(itemDto.getEtc());

        return novel;
    }

    private static Magazine createMagazine(ItemDto itemDto) {
        Magazine magazine = new Magazine();

        magazine.setName(itemDto.getName());
        magazine.setAuthor(itemDto.getAuthor());
        magazine.setPrice(itemDto.getPrice());
        magazine.setStockQuantity(itemDto.getStockQuantity());
        magazine.setTheme(itemDto.getEtc());

        return magazine;
    }

    private static Reference createReference(ItemDto itemDto) {
        Reference reference = new Reference();

        reference.setName(itemDto.getName());
        reference.setAuthor(itemDto.getAuthor());
        reference.setPrice(itemDto.getPrice());
        reference.setStockQuantity(itemDto.getStockQuantity());
        reference.setSubject(itemDto.getEtc());

        return reference;
    }

    public List<Register> findMyRegisters(Long memberId) {
        return registerRepository.findMyRegisters(memberId);
    }
}
