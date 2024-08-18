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

---

# API 개발 고급 - 컬렉션 조회 최적화

### 스프링 3.0 이상 & 하이버네이트 6 다른점

1) OrderApiController v1, v2 하이버네이트6 버그

* delivery 조회 할때 Order를 JsonIgnore 넣었으나 delivery_id로 order 조회 하는 버그

2) 스프링 3.0 이상부터는 OrderApiController v3 API는 중복 데이터(카테시안 곱)가 발생하지 않는다. (쿼리로 날리면 중복으로 나옴)

* 스프링 3.0 이상부터는 fetch join할때 자동으로 distinct(중복제거) 기능을 제공해줌. JPQL 실행전 자동으로 distinct 넣는다.
  함

### 패치 조인의 한계

***패치 조인의 전제 조건 : 무조건 주인객체(메인 진짜 객체)에 들어있는 모든 참조객체(패치조인 대상)를 가져온다.***

**따라서 패치조인 대상에 조건을 주면 안된다.(물론, 주인 객체에 영향을 주지 않고 trade off 해서 성능을 위해 어쩔수없이 조회하는 용도는 괜찬다)**

1) 조인 대상에 별칭을 줄수가 없다.(JPA 표준 스펙)
    * 하이버네이트는 가능하나 왠만하면 사용하지 말자

```text
그 이유는 별칭을 준 이후 on(또는 where)에서 별칭으로 조건을 주면 OneToMany 관계에서 Collection 형태로 조회되는 데이터가 전부 조회되지 않고 일부만 나오기 때문에 문제가 생길 수 있다 -> 문제가 4번으로 이어짐
```

2) 둘 이상의 컬렉션은 패치 조인을 할수 없다.

3) 컬렉션을 패치조인하면 페이징 API를 사용할 수 없다.

4) fetch join의 대상은 on, where 등에서 필터링 조건으로 사용하면 안된다.

```text
왜냐하면 JPA의 엔티티 객체 그래프는 DB와 데이터 일관성을 유지해야 하기 때문입니다.

예를 들어서 DB에 데이터가 다음과 같이 있습니다.

team1 - memberA

team1 - memberB

team1 - memberC

그런데 조인 대상의 필터링을 제공해서 조회결과가 memberA, memberB만 조회하게 되면 JPA 애플리케이션은 다음과 같은 결과로 조회됩니다.

team1 - {memberA, memberB}

team1에서 회원 데이터를 찾으면 memberA, memberB만 반환되는 것이지요.

이렇게 되면 JPA 입장에서 DB와 데이터 일관성이 깨지고, 최악의 경우에 memberC가 DB에서 삭제될 수도 있습니다.

왜냐하면 JPA의 엔티티 객체 그래프는 DB와 데이터 일관성을 유지해야 하기 때문입니다! 잘 생각해보면 우리가 엔티티의 값을 변경하면 DB에 반영이 되어버리지요.

정리하면 JPA의 엔티티 데이터는 DB의 데이터와 일관성을 유지해야 합니다. 내가 임의로 데이터를 빼고 조회해버리면 DB에 해당 데이터가 없다고 판단하는 것과 똑같습니다.

그래서 엔티티를 사용할 때는 이 부분을 매우 조심해야 합니다! fetch join에서 제공하지 않는 이유도 이 데이터 일관성이 깨지기 때문에 제공하지 않습니다.

그러면 어떻게 해결할 수 있을까요? 바로 엔티티가 아닌 그냥 값으로 조회하면 됩니다. 엔티티는 객체 그래프를 유지하고 DB와 데이터 일관성을 유지합니다. 그런데 엔티티가 아닌 일반 값들은 그럴 필요가 없지요^^

일반 DB 조회하듯이 필요한 값을 나열해서 조회하면 됩니다.

예를 들어서 DTO 같은 것으로 조회하면 되는 것이지요.

물론 이 DTO안에 또 엔티티를 넣거나 그러지는 말고, 정말 일반 DB 조회하듯이 값을 풀어서 조회하면 됩니다.

```

### 컬렉션 패치 조인 단점

1) 페이징 처리가 불가능

패치 조인에 페이징 처리 기능 사용하면 다음과 같은 warning을 볼수 있다.

```text
org.hibernate.orm.query : HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory

```

2) 컬렉션 패치 조인은 1개만 사용할 수 있다. 둘 이상은 하면 안된다. 데이터가 부정합하게 조회될 수 있다.
























