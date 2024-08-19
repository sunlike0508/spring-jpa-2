package jpabook.jpashop.api;


import java.time.LocalDateTime;
import java.util.List;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import jpabook.jpashop.service.OrderSearch;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;


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


    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> orders3_page(@RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {

        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        orders.forEach(order -> System.out.println("주문 ID : " + order.getId() + " class : " + order));

        return orders.stream().map(OrderDto::new).toList();
    }


    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> orders4() {

        return orderQueryRepository.findOrderQueryDtos();
    }


    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> orders5() {

        return orderQueryRepository.findAllByDto_Optimization();
    }


    @GetMapping("/api/v6/orders")
    public List<OrderFlatDto> orders6() {
        // 아이템 개수만큼 나옴. 우리가 원하는건 주문 1개에 아래 item 있는 스팩을 원함
        return orderQueryRepository.findAllByDto_flat();
    }


    @GetMapping("/api/v6.1/orders")
    public List<OrderQueryDto> orders6_spec() {

        List<OrderFlatDto> orderFlatDtos = orderQueryRepository.findAllByDto_flat();

        return orderFlatDtos.stream().collect(groupingBy(
                        o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(),
                                o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()),
                                toList()))).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(),
                        e.getKey().getOrderStatus(), e.getKey().getAddress(), e.getValue())).toList();
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

        private String itemName;
        private int orderPrice;
        private int count;


        public OrderItemDto(OrderItem orderItem) {
            this.itemName = orderItem.getItem().getName();
            this.orderPrice = orderItem.getOrderPrice();
            this.count = orderItem.getCount();
        }
    }
}
