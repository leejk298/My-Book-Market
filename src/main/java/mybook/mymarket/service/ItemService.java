package mybook.mymarket.service;

import lombok.RequiredArgsConstructor;
import mybook.mymarket.domain.item.Item;
import mybook.mymarket.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service    // 스프링빈에 등록
@Transactional(readOnly = true) // 읽기전용: 리소스 낭비 X, default: false
@RequiredArgsConstructor    // final 키워드를 가진 필드(itemRepository)로 생성자를 만들어줌
public class ItemService {
    private final ItemRepository itemRepository;    // 스프링이 스프링 빈에 있는 itemRepository 를 주입해줌

    /**
     * 상품 조회 => 데이터 변경 X
     */
    public List<Item> findItems() {
        return itemRepository.findAll();
    }

    public Item findOne(Long itemId) {
        return itemRepository.findOne(itemId);
    }

    /**
     * 변경 감지 (dirty checking)
     */
    @Transactional
    public void updateItem(Long itemId, String name, int price, int stockQuantity) {
        // 서비스 계층에서 itemId 를 기반으로 실제 DB에 있는 영속 상태의 findItem 을 찾아옴
        Item findItem = itemRepository.findOne(itemId);     // 영속 상태

        findItem.changeItem(name, price, stockQuantity);    // 변경 감지
        // 위처럼 구현하면 어디서 변경되는 지 찾을 수 있음
        // 그러므로 엔티티 레벨에서 changeItem() 구현하여 사용하자
        // setter 를 사용하면 어디서 변경되는 지 헷갈림 => 유지보수 힘듬
//        findItem.setName(param.getName());
//        findItem.setPrice(param.getPrice());
//        findItem.setStockQuantity(param.getStockQuantity());

        /** itemRepository.save(findItem);
         *  이 코드를 쓸 필요가 없음 => 이미 영속 상태의 findItem 을 찾아왔으므로
         @Transactional 에 의해 commit 이 일어남 => commit 시점에 JPA 가 flush() 날림
         => JPA 가 관리를 함, 변경 감지 기능 (dirty checking) 을 통해
         update query 가 나가게 됨
         */
    }
}