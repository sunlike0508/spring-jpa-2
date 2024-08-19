package jpabook.jpashop;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.Delivery;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.item.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class IntiDb {

    private final InitService initService;


    @PostConstruct
    public void init() {
        // 여기다가 dbInti1 로직을 넣어도 될것 같지만 스프링 어노테이션(PostConstruct) 아래에서 잘 안먹힌다.

        initService.dbInit1();
        initService.dbInit2();
    }


    @Component
    @Transactional
    @RequiredArgsConstructor
    static class InitService {

        private final EntityManager em;


        public void dbInit1() {
            Member member = Member.createMember("userA", "서울", "1", "1111");
            em.persist(member);

            Book book = Book.createBook("JAP Book 1 ", 10000, 100);
            em.persist(book);

            Book book2 = Book.createBook("JAP Book 2 ", 20000, 100);

            em.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderItem(book, 10000, 1);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 20000, 2);

            Delivery delivery = new Delivery();
            delivery.setAddress(member.getAddress());

            Order order = Order.createOrder(member, delivery, orderItem1, orderItem2);

            em.persist(order);
        }


        public void dbInit2() {
            Member member = Member.createMember("userB", "의정부", "2", "222");
            em.persist(member);

            Book book = Book.createBook("Spring Book 3 ", 20000, 100);
            em.persist(book);

            Book book2 = Book.createBook("Spring Book 4 ", 30000, 100);

            em.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderItem(book, 20000, 2);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 30000, 4);

            Delivery delivery = new Delivery();
            delivery.setAddress(member.getAddress());

            Order order = Order.createOrder(member, delivery, orderItem1, orderItem2);

            em.persist(order);
        }
    }

}