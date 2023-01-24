//package mybook.mymarket;
//
//
//import lombok.RequiredArgsConstructor;
//import mybook.mymarket.domain.*;
//import mybook.mymarket.domain.item.Item;
//import mybook.mymarket.domain.item.Magazine;
//import mybook.mymarket.domain.item.Novel;
//import mybook.mymarket.domain.item.Reference;
//import mybook.mymarket.service.LoginService;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import javax.annotation.PostConstruct;
//import javax.persistence.EntityManager;
//
///**
// * 사용자: 이정규, 김정규, 박정규, 심정규
// * 상품(책): JPA, 스프링, 알고리즘, 토익스피킹
// * 등록
// * => 이: JPA 10권 20000원 , 스프링 20권 15000원
// * => 김: 알고리즘 15권 10000원, 토익스피킹 30권 5000원
// * => 박: JPA 20권, 토익스피킹 40권
// * => 심: 스프링 25권, 알고리즘 50권
// * 주문
// * => 이: 알고리즘(김) 5 직 - 10 거래완료 / 토익스피킹(박) 10 배 - 30 거래대기
// * => 김: JPA(이) 10 직 - 0 거래완료 / 스프링(심) 5 배 - 20 거래대기
// * => 박: 스프링(이) 10 직 - 10 거래완료 / 알고리즘(심) 20 배 - 30 거래대기
// * => 심: JPA(박) 10 직 - 10 거래완료 / 토익스피킹(김) 10 배 - 20 거래대기
// */
//
//@Component  // 스프링이 스프링 컨테이너에 빈을 등록
//@RequiredArgsConstructor    // initService 주입
//public class InitDb {
//    private final InitService initService;
//    private final LoginService loginService;
//
//    @PostConstruct  // 종속성 주입이 완료된 후 실행되어야 하는 메소드에 사용
//    public void init() {
//        // 별도의 빈으로 생성해야함 => 스프링 라이프사이클 때문에
//        initService.dbInit1();  // Db 초기화함수 호출
////        initService.dbInit2();
//    }
//
//    @Component  // 별도의 빈으로 생성하기 위해 내부 클래스로
//    @Transactional  // 데이터 변경이 일어나므로
//    @RequiredArgsConstructor    // em 주입
//    static class InitService {
//        private final EntityManager em;
//        public void dbInit1() { // Db 초기화함수
//            Member member = createMember("leejk", "1234", "이정규", "천안", "청수동", "31194");
//
//            Reference reference = createReference("JPA", "Kim", 20000, 10, "프로그래밍");
//            Register register =  Register.createRegister(member, reference);
//            em.persist(register);
//
//            Magazine magazine = createMagazine("스프링", "Kim", 15000, 20, "패션");
//            Register register1 = Register.createRegister(member, magazine);
//            em.persist(register1);
//
//            Novel novel = createNovel("알고리즘", "Kim", 10000, 15, "판타지");
//            Reference reference1 = (Reference) createReference("토익스피킹", "Kim", 5000, 40, "영어");
//
//            OrderItem orderItem = OrderItem.createOrderItem(novel, 10000, 5);
//            OrderItem orderItem1 = OrderItem.createOrderItem(reference1, 5000, 10);
//
//            Deal deal = new Deal();
//            deal.setStatus(DealStatus.WAIT);
//            deal.setType(DealType.DIRECT);
//
//            Order order = Order.createOrder(member, deal, orderItem);
//            em.persist(order);
//
//            Deal deal1 = new Deal();
//            deal1.setStatus(DealStatus.WAIT);
//            deal.setType(DealType.DELIVERY);
//
//            Order order1 = Order.createOrder(member, deal, orderItem1);
//            em.persist(order1);
//        }
//
//        private static Member createMember(String nickName, String password, String userName, String city, String street, String zipcode) {
//            Member member = new Member();
//
//            member.setNickName(nickName);
//            member.setPassword(password);
//            member.setUserName(userName);
//            member.setAddress(new Address(city, street, zipcode));
//
//            return member;
//        }
//
//
//        private static Reference createReference(String name, String author, int price, int stockQuantity, String etc) {
//            Reference reference = new Reference();
//
//            reference.setName(name);
//            reference.setAuthor(author);
//            reference.setPrice(price);
//            reference.setStockQuantity(stockQuantity);
//            reference.setSubject(etc);
//
//            return reference;
//        }
//
//        private static Magazine createMagazine(String name, String author, int price, int stockQuantity, String etc) {
//            Magazine magazine = new Magazine();
//
//            magazine.setName(name);
//            magazine.setAuthor(author);
//            magazine.setPrice(price);
//            magazine.setStockQuantity(stockQuantity);
//            magazine.setTheme(etc);
//
//            return magazine;
//        }
//
//        private static Novel createNovel(String name, String author, int price, int stockQuantity, String etc) {
//            Novel novel = new Novel();
//
//            novel.setName(name);
//            novel.setAuthor(author);
//            novel.setPrice(price);
//            novel.setStockQuantity(stockQuantity);
//            novel.setGenre(etc);
//
//            return novel;
//        }
//    }
//}
