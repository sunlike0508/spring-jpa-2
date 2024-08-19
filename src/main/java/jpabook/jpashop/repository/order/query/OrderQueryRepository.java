package jpabook.jpashop.repository.order.query;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final EntityManager em;


    public List<OrderQueryDto> findOrderQueryDtos() {
        List<OrderQueryDto> result = findOrders();

        result.forEach(o -> {
            List<OrderItemQueryDto> orderItems = findOrderItems(o.getOrderId());
            o.setOrderItemQueryDtos(orderItems);
        });

        return result;
    }


    private List<OrderItemQueryDto> findOrderItems(Long orderId) {
        return em.createQuery(
                        "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count) "
                                + "from OrderItem oi join oi.item i where oi.order.id = :orderId", OrderItemQueryDto.class)
                .setParameter("orderId", orderId).getResultList();
    }


    public List<OrderQueryDto> findOrders() {

        return em.createQuery(
                "select new jpabook.jpashop.repository.order.query.OrderQueryDto(o.id, m.name, o.orderDate, o.status, d.address) from Order o join o.member m"
                        + " join o.delivery d", OrderQueryDto.class).getResultList();

    }


    public List<OrderQueryDto> findAllByDto_Optimization() {

        List<OrderQueryDto> orders = findOrders();

        List<Long> orderIds = orders.stream().map(OrderQueryDto::getOrderId).toList();

        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(orderIds);

        orders.forEach(o -> o.setOrderItemQueryDtos(orderItemMap.get(o.getOrderId())));

        return orders;
    }


    private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long> orderIds) {
        List<OrderItemQueryDto> orderItems = em.createQuery(
                        "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count) "
                                + "from OrderItem oi join oi.item i where oi.order.id in :orderIds", OrderItemQueryDto.class)
                .setParameter("orderIds", orderIds).getResultList();

        return orderItems.stream().collect(Collectors.groupingBy(OrderItemQueryDto::getOrderId));
    }


    // 페이징 기능은 가능하나 올바른 페이징은 불가능
    // 장점은 쿼리 하나
    public List<OrderFlatDto> findAllByDto_flat() {

        return em.createQuery(
                "select distinct new jpabook.jpashop.repository.order.query.OrderFlatDto(o.id, m.name, o.orderDate, "
                        + "o.status, d.address, i.name, oi.orderPrice, oi.count) "
                        + "from Order o join o.member m join o.delivery d join o.orderItems oi join oi.item i",
                OrderFlatDto.class).getResultList();
    }
}
