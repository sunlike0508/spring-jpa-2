# API 고급 개발 편

1) 절대 엔티티를 API 응답 값으로 반환하지 마라.

    * 무한 루프 문제, N+1 문제, 지연 로딩으로 인한 참조(프록시) 객체 바인딩 문제
    * 하이버네이트 이해를 높이기 위해 테스트에서 Hibernate5JakartaModule 사용
    * 연관관계 걸린 곳은 @JsonIgnore를 사용

2) 1번 규칙을 위해 DTO를 사용해라.

3) 최종적으론 패치 조인을 사용해라.

```java
class Repository {

    // fetch join
    public List<Order> findAllWithMemberDelivery() {
        return em.createQuery("select o from Order o join fetch o.member m join fetch  o.delivery d", Order.class)
                .getResultList();
    }


    // 직접 join
    public List<OrderSimpleQueryDto> findOrderDtos() {
        return em.createQuery(
                "select new jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address) "
                        + "from Order o join o.member m join o.delivery d", OrderSimpleQueryDto.class).getResultList();
    }
}
```

* fetch join의 단점은 내가 원하는 데이터까지 모두 select 한다.
* 그러면 직접 join을 사용해서 필요한 데어터만 select한다. fetch join은 엔티티, Embedded만 가져올 수 있다.
  그래서 직접 join을 통해 fit하게 만들어야 한다.
    * 성능적으로는 직접 join이 좋다. 그러나 fetch join은 객체 그래프 탐색을 하거나 유연한 코드를 작성할 수 없다.
    * 사실 요즘 서버가 너무 성능이 좋기때문에 성능차이는 사실 그닥... 뭐 적당히 trade-off 하자.
* 진짜 그래도 성능이 너무 안나온다... 순수 SQL 및 jdbc 사용.... 





















