package jpabook.jpashop.repository;


import java.util.List;
import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.service.OrderSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;


    public void save(Order order) {
        em.persist(order);
    }


    public Order findOne(Long orderId) {
        return em.find(Order.class, orderId);
    }


    public List<Order> findAll(OrderSearch orderSearch) {

        if(orderSearch.getOrderStatus() == null || !StringUtils.hasText(orderSearch.getMemberName())) {
            return em.createQuery("select o from Order o join o.member m", Order.class).getResultList();
        }

        return em.createQuery(
                        "select o from Order o join o.member m where o.status = :status and m.name like concat('%', :name, '%')",
                        Order.class).setParameter("status", orderSearch.getOrderStatus())
                .setParameter("name", orderSearch.getMemberName()).setMaxResults(1000).getResultList();
    }


    public List<Order> findAllWithMemberDelivery() {

        return em.createQuery("select o from Order o join fetch o.member m join fetch  o.delivery d", Order.class)
                .getResultList();
    }


    public List<Order> findAllWithItem() {

        // 스프링 3.0 이하 버전에는 distinct 붙어줘야 함
        //        return em.createQuery("select distinct o from Order o join fetch o.member m join fetch o.delivery d"
        //                + " join fetch o.orderItems oi join fetch oi.item i", Order.class).getResultList();

        return em.createQuery("select o from Order o join fetch o.member m join fetch o.delivery d"
                + " join fetch o.orderItems oi join fetch oi.item i", Order.class).getResultList();
    }


    public List<Order> findAllWithMemberDelivery(int offset, int limit) {
        return em.createQuery("select o from Order o join fetch o.member m join fetch  o.delivery d", Order.class)
                .setFirstResult(offset).setMaxResults(limit).getResultList();
    }
}
