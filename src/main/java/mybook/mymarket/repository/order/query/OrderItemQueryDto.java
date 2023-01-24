package mybook.mymarket.repository.order.query;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class OrderItemQueryDto {
    @JsonIgnore
    private Long orderId;
    private Long itemId;
    private String itemName;
    private String registerMemberName;
    private int orderPrice;
    private int count;

    public OrderItemQueryDto(Long orderId, Long itemId, String itemName, String registerMemberName, int orderPrice, int count) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.registerMemberName = registerMemberName;
        this.orderPrice = orderPrice;
        this.count = count;
    }
}
