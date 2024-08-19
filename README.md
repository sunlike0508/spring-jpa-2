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

    * Order 기준으로 Item 패치조인하면 Order는 하나인데 Item은 여러개인다. 근데 페이징 기준은 Order 기준으로 하려고 한다.
    * 그러나 item이 여러개이기 때문에 item 기준으로 바뀐다.
    * 해결은 아래 작성해둠

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

### 컬렉션 패치 조인 한계 돌파

1) OnetoOne, ManytoOne 관계들만 모두 조인을 한다.
2) 컬렉션은 지연 로딩으로 조회한다.
3) 지연 로딩 성능 최적화를 위해 프로퍼티에 글로벌로 적용를 적용한다.

```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=100

```

* @BatchSize을 통해 개별 컬렉션 최적화해도 된다. (ex : OneToMany가 걸린 orderItems), ManyToOne 관계는 class 위에 적용 (ex : item)

#### 하이버네이트 6.2 이상에서 발견되는 batchSize 만큼 in 절에 null이 들어감

버그는 아니고 하이버네이트에서 성능 최적화로 일부러 만들어진 쿼리

```sql
#
batch size 5로 했을 경우
select oi1_0.order_id, oi1_0.order_item_id, oi1_0.count, oi1_0.item_id, oi1_0.order_price
from order_item oi1_0
where oi1_0.order_id in (1, 2, NULL, NULL, NULL);
```

``` textmate
스프링 부트 3.1 부터는 하이버네이트 6.2를 사용하는데요.

하이버네이트 6.2 부터는 where in 대신에 array_contains를 사용합니다.

where in 사용 문법

where item.item_id in(?,?,?,?)

array_contains 사용 문법

where array_contains(?,item.item_id)

침거러 where in에서 array_contains를 사용하도록 변경해도 결과는 완전히 동일합니다. 그런데 이렇게 변경하는 이유는 성능 최적화 때문입니다.

select ... where item.item_id in(?)

SQL을 실행할 때 데이터베이스는 SQL 구문을 이해하기 위해 SQL을 파싱하고 분석하는 등 여러가지 복잡한 일을 처리해야 합니다. 그래서 성능을 최적화하기 위해 이미 실행된 SQL 구문은 파싱된 결과를 내부에 캐싱하고 있습니다.

이렇게 해두면 다음에 같은 모양의 SQL이 실행되어도 이미 파싱된 결과를 그대로 사용해서 성능을 최적화 할 수 있습니다.

참고로 여기서 말하는 캐싱은 SQL 구문 자체를 캐싱한다는 뜻이지 SQL의 실행 결과를 캐싱한다는 뜻이 아닙니다.

SQL 구문 차제를 캐싱하기 때문에 여기서 ?에 바인딩 되는 데이터는 변경되어도 캐싱된 SQL 결과를 그대로 사용할 수 있습니다.

그런데 where in 쿼리는 동적으로 데이터가 변하는 것을 넘어서 SQL 구문 자체가 변해버리는 문제가 발생합니다.

다음 예시는 in에 들어가는 데이터 숫자에 따라서 총 3개의 SQL구문이 생성됩니다.

where item.item_id in(?)

where item.item_id in(?,?)

where item.item_id in(?,?,?,?)

SQL 입장에서는 ?로 바인딩 되는 숫자 자체가 다르기 때문에 완전히 다른 SQL입니다. 따라서 총 3개의 SQL 구문이 만들어지고, 캐싱도 3개를 따로 해야 합니다. 이렇게 되면 성능 관점에서 좋지 않습니다.

array_contains를 사용하면 이런 문제를 깔끔하게 해결할 수 있습니다.

이 문법은 결과적으로 where in과 동일합니다. array_contains은 왼쪽에 배열을 넣는데, 배열에 들어있는 숫자가 오른쪽(item_id)에 있다면 참이 됩니다.

예시) 다음 둘은 같다.

select ... where array_contains([1,2,3],item.item_id)

select ... where item.item_id in(1,2,3)

이 문법은 ?에 바인딩 되는 것이 딱1개 입니다. 배열1개가 들어가는 것이지요.

select ... where array_contains(?,item.item_id)

따라서 배열에 들어가는 데이터가 늘어도 SQL 구문 자체가 변하지 않습니다. ?에는 배열 하나만 들어가면 되니까요.

이런 방법을 사용하면 앞서 이야기한 동적으로 늘어나는 SQL 구문을 걱정하지 않아도 됩니다.

결과적으로 데이터가 동적으로 늘어나도 같은 SQL 구문을 그대로 사용해서 성능을 최적화 할 수 있습니다.

참고로 array_contains에서 default_batch_fetch_size에 맞추어 배열에 null 값을 추가하는데, 이 부분은 아마도 특정 데이터베이스에 따라서 배열의 데이터 숫자가 같아야 최적화가 되기 때문에 그런 것으로 추정됩니다.
```

#### @EqualsAndHashCode

equals 는 객체가 같은 객체인지 비교(주소값 비교) : 동등성

hashCode는 객체안의 내부 필드 값이 같은지 비교 : 동일

```textmate
Equality made easy: Generates hashCode and equals implementations from the fields of your object.
클래스 위에 정의하면, 컴파일 시점에 자동으로 객체의 필드로부터 HashCode와 Equals를 오버라이딩하여 구현해주는 애노테이션
클래스에 있는 모든 필드들에 대한 비교를 수행함

of 속성 : 특정 값만 정해서 equals, hashCode 비교
```

---

# OSIV (Open Session In View) : 하이버네이트

Open EntityManager In View : JPA

Spring 어플리케이션을 키면 아래와 같은 로그를 볼수 있다.

```shell
WARN 79130 --- [  restartedMain] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
```

### 상세 설명

관례상 OSIV라고 한다.

<img width="913" alt="Screenshot 2024-08-19 at 15 16 12" src="https://github.com/user-attachments/assets/b1937b2c-ed70-47a2-9101-d3a1d34421fb">

OSIV 전략은 트랜잭션 시작처럼 최초 데이터베이스 커넥션 시작시점부터 API 응답이 끝날때까지 영속성 컨텍스트와 DB 커넥션을 유지힌다.

그래서 지연로딩이 가능한 것이다. (repository로 최초 db 조회 후에 api 요청이 끝날때까지 커넥션을 유지하면서 다른 쿼리 커맨드가 db 커넥션을 사용할 수 있게 한다.)

### 단점 : 너무 오래동안 커넥션을 잡고 있다.

그래서 커넥션 풀이 마를 수 있다. 그렇다면 요청이 많아지면 대기 시간이 길어진다.

### OSIV를 OFF 하면?

트랜잭션 안에서 모든 db 처리(지연로딩)를 끝내야 한다.

<img width="902" alt="Screenshot 2024-08-19 at 15 03 10" src="https://github.com/user-attachments/assets/204e0ce8-d8ce-4680-87c7-f084313ac445">

### 해결책

조회와 명령의 쿼리를 분리. 그래서 실시간이 많은 곳들에 대해서 트랜잭션을 범위를 최소화하고 OSIV를 off 한다.















