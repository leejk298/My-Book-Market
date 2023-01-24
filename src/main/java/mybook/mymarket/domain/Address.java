package mybook.mymarket.domain;

import lombok.Getter;

import javax.persistence.Embeddable;

@Getter
@Embeddable // 내장되어질 수 있는
public class Address {
    private String city;
    private String street;
    private String zipcode;

    /**
     * JPA 기본스펙이 객체 생성 시 리플렉션과 프록시 같은 기술들을 사용해야 하는데
     * 기본 생성자가 없으면 해당 기술을 사용할 수 없으므로 기본 생성자를 만들어줘야함
     * Default Constructor
     */
    protected Address() {   // JPA 스펙에서는 protected 까지 허옹
    }

    /**
     * Parameter Constructor
     * 값 타입은 변경이 가능하도록 설계하면 안됨 => Setter X => 변경 불가능
     * 생성할 때만 값 세팅하도록 => 생성자
     */
    public Address(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }
}