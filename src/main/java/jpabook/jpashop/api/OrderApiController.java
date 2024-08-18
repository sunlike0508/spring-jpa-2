package jpabook.jpashop.api;


import java.time.LocalDateTime;
import java.util.List;
import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.service.OrderSearch;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    public final OrderRepository orderRepository;


    @GetMapping("/api/v1/orders")
    public List<Order> orders1() {
        List<Order> orders = orderRepository.findAll(new OrderSearch());

        for(Order order : orders) {
            order.getMember().getName();
            order.getDelivery().getAddress();

            List<OrderItem> orderItems = order.getOrderItems();

            orderItems.forEach(orderItem -> orderItem.getItem().getName());
        }


        return orderRepository.findAll(new OrderSearch());
    }


    @GetMapping("/api/v2/orders")
    public List<OrderDto> orders2() {

        System.out.println(">> 전체 주문 조회");
        List<Order> orders = orderRepository.findAll(new OrderSearch());

        return orders.stream().map(OrderDto::new).toList();
    }


    @GetMapping("/api/v3/orders")
    public List<OrderDto> orders3() {
        List<Order> orders = orderRepository.findAllWithItem();

        orders.forEach(order -> System.out.println("주문 ID : " + order.getId() + " class : " + order));

        return orders.stream().map(OrderDto::new).toList();
    }


    @Data
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;


        public OrderDto(Order order) {
            orderId = order.getId();
            System.out.println(">> 맴버 조회 : " + orderId);
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            System.out.println(">> 배송 조회 : " + orderId);
            address = order.getDelivery().getAddress();
            System.out.println(">> 배송 조회 끝");

            orderItems = order.getOrderItems().stream().map(OrderItemDto::new).toList();
        }
    }


    @Data
    static class OrderItemDto {

        private Long orderItemId;
        private int orderPrice;
        private int count;


        public OrderItemDto(OrderItem orderItem) {
            this.orderItemId = orderItem.getId();
            this.orderPrice = orderItem.getOrderPrice();
            this.count = orderItem.getCount();
        }
    }
}
