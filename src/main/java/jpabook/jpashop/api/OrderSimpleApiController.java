package jpabook.jpashop.api;


import java.time.LocalDateTime;
import java.util.List;
import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryRepository;
import jpabook.jpashop.service.OrderSearch;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ManytoOne, OneToOne 성능 최적화
 */

@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;


    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {

        List<Order> orders = orderRepository.findAll(new OrderSearch());

        for(Order order : orders) {
            order.getMember().getName(); //
            order.getDelivery().getAddress(); // 강제 초기화
        }

        return orders;
    }


    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {

        System.out.println(" >> 전체 주문 조회");
        List<Order> orders = orderRepository.findAll(new OrderSearch());

        return orders.stream().map(SimpleOrderDto::new).toList();
    }


    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {

        List<Order> orders = orderRepository.findAllWithMemberDelivery();

        return orders.stream().map(SimpleOrderDto::new).toList();
    }


    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4() {

        return orderSimpleQueryRepository.findOrderDtos();
    }


    @Data
    public class SimpleOrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;


        public SimpleOrderDto(Order order) {
            this.orderId = order.getId();
            System.out.println(" >> 맴버 조회");
            this.name = order.getMember().getName();
            this.orderDate = order.getOrderDate();
            this.orderStatus = order.getStatus();

            System.out.println(" >> 배송 조회");
            this.address = order.getDelivery().getAddress();
        }
    }

}
