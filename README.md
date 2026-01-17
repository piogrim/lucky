# lucky
=======

# 2026.1.16 부하테스트 내용이 추가되었습니다. 
# [부하 테스트로 건너뛰기](#load-test)

아키텍처 다이어그램(2026.1.15)


<img width="2654" height="1468" alt="프로젝트 다이어그램 (1)" src="https://github.com/user-attachments/assets/2b711561-0f55-40c6-abab-4a0dbd44f157" />

일반적으로 쿠버네티스에서 컨테이너 런타임으로 containerd를 사용하나 본 프로젝트는 minikube를 이용하고 컨테이너 런타임으로 docker를 사용했습니다.

## 아키텍처 흐름
모든 요청은 **Nginx**를 통해 들어와 **Gateway**를 거쳐 **User Service**, **Board Service**로 전달됩니다.
 **쿠버네티스를 도입함으로 분산 환경에서 기능하는 것이 가능해졌습니다.**

1. **Nginx (Port 80)**
   - 모든 외부 HTTP 요청을 수신
   - **lucky-gateway**로 요청을 전달

2. **lucky-gateway(Spring Cloud Gateway)**
   - 요청 경로를 분석하여 특정 서비스로 라우팅
   - `/user-service/**` 경로의 요청을 **user-service**로 전달

3. **user-service**
   - 회원가입, 로그인, JWT 발급 및 인증/인가 로직 수행

4. **board-service**
   - Gateway가 검증하고 넘겨준 X-User-Name 헤더를 통해 작성자를 식별
   - 게시글 CRUD 로직 수행

---

## Kubernetes를 도입하기까지

Docker → Docker-compose → Kubernetes

• 환경(개발, 테스트, 배포) 종속성 문제 해결을 위해 Docker 도입

• 기존 명령형 방식을 통한 도커 컨테이너 생성은 반복(비효율)적이고 명령어 작성시 실수할 수 있는 문제가 존재 → Docker-Compose 도입

• Docker-Compose는 단일 호스트머신에서만 동작할 수 있기 때문에 추후 대규모 트래픽 처리를 위한 분산 환경에서는 사용이 불가능 → 분산 환경에서의 배포 관리를 위한 Kubernetes 도입

---

## 2026.1.15 아키텍처 및 API 명세(Order-service, Inventory-service 추가)

### Order-Service와 Inventory-Service 그리고 Kafka

### 요구 사항
1. **다중 상품 주문**  
   사용자는 여러 종류의 상품을 한 번에 주문할 수 있어야 한다.
2. **주문 조회**  
   사용자는 자신의 주문 진행 상태(`PENDING`, `SUCCESS`, `CANCELED`)를 조회할 수 있어야 한다.
3. **트랜잭션 보장**  
   주문은 재고 확인 및 차감 과정을 거쳐야 하며,  
   부분 실패 없이 원자적(Atomic)으로 처리되어야 한다.

### 구현 방식

### 1. 주문 생성
- 주문 서비스(Order-Service)는 요청받은 주문을  
  `PENDING` 상태로 생성 및 저장한다.

### 2. 이벤트 발행
- 주문 서비스는 주문에 포함된 각 상품 정보를  
  Kafka의 `order_create` 토픽으로 발행한다.

### 3. 재고 처리
- 재고 서비스(Inventory-Service)는 Kafka 메시지를 소비하여  
  각 상품에 대해 재고 차감을 수행한다.

### 4. 롤백
- 여러 상품 중 하나라도 재고 차감에 실패할 경우:
  - 이미 차감에 성공한 다른 상품들의 재고를  
    롤백합니다.

### 5. 주문 상태 업데이트
- 재고 차감 실패 시
  - 주문 서비스에 실패 결과 전송
  - 주문 상태를 `CANCELED`로 업데이트
- 재고 차감 성공 시
  - 주문 서비스에 성공 결과 전송
  - 주문 상태를 `SUCCESS`로 업데이트


### 주문 조회
- 주문 조회 시,  
  재고 서비스가 Kafka를 통해 전달한 결과 메시지를 기반으로  
  주문 상태가 `SUCCESS` 또는 `CANCELED`인 주문을 확인할 수 있다.


### Kafka를 사용한 이유

1. 주문 서비스와 재고 서비스는 서로 다른 생명 주기를 가지며  
   별개의 컨테이너에서 실행
2. 별개의 컨테이너에서 실행되므로  
   주문 서비스와 재고 서비스 간 통신 수단이 필요
3. 동기 방식으로 주문 서비스가 재고 서비스의 API를 직접 호출할 경우:
   - 서비스 간 결합도가 높아짐
   - 재고 서비스 장애 시 주문 서비스도 영향을 받음.
4. 이러한 문제를 해결하기 위해  
   중간에 브로커를 두는 비동기 메시지 기반 통신 방식을 선택
5. RabbitMQ와 Kafka 중  
   대량 데이터 처리에 유리한 Kafka를 선택

<div id="load-test"></div>

## 부하 테스트

대규모 트래픽 상황에서 주문 시스템의 안정성과 데이터 정합성을 검증하기 위해 **k6**를 사용하여 부하 테스트를 수행했습니다.
k6-test-script/order-test-10k.js를 사용했습니다.

#### 테스트 환경 및 설정
* **테스트 도구:** k6
* **시나리오 실행기:** `shared-iterations` (총 목표 요청 수를 설정하여 수행)
* **동시 접속자 (VUs):** 5,000명
* **총 요청 수 (Iterations):** 20,000건

#### 테스트 시나리오
1.  **동시성 부하:** 5,000명의 가상 유저가 동시에 `/api/orders`에 주문 요청을 전송.
2.  **무작위 데이터:**
    * 사용자 ID: 1~1,000 사이 랜덤 (헤더의 X-User-Name값)
    * 상품 ID: 1~100 사이 랜덤 (모든 상품은 재고가 300개입니다.)
    * 주문 수량: 1~3개 랜덤
3.  **검증 목표:** API 응답(200 OK) 확인 및 백그라운드(Kafka, Inventory)에서의 데이터 처리 완결성 검증.

#### 테스트 목적
* **비동기 처리 검증:** Kafka를 통한 비동기 주문 처리가 트래픽을 버퍼링하고 안정적으로 처리하는지 확인.
* **동시성 제어:** 인기 상품(ID 1~100)에 주문이 몰릴 때, Inventory DB의 동시성 이슈(Race Condition) 없이 재고가 정확하게 차감되는지 확인.
* **데이터 정합성:** `SUCCESS`와 `CANCELED` 상태의 최종 주문 데이터와 재고 데이터가 일치하는지 검증.

## 테스트 결과

### 1. 주문 처리 결과 조회 (Order DB)
<img width="508" height="95" alt="스크린샷 2026-01-16 오후 5 18 51" src="https://github.com/user-attachments/assets/148f08db-7ef2-478c-8f88-fe7d8b08e014" />

* 총 20,000건의 대량 주문 요청이 들어왔으나 누락(PENDING) 없이 모두 처리되었습니다. 재고가 있는 만큼은 `SUCCESS`(약 1.5만 건), 재고 부족분은 `CANCELED`(약 0.5만 건)로 정확하게 분류되어 데이터 유실이 없음을 확인했습니다.

### 2. 판매 수량 정합성 검증 (Order DB)
<img width="390" height="240" alt="스크린샷 2026-01-16 오후 5 19 37" src="https://github.com/user-attachments/assets/169fd63d-16cc-45c2-b458-0ee2feb9e895" />

* 주문 성공(`SUCCESS`) 상태인 건들의 상품별 판매 수량 총합을 조회했습니다. 모든 상품이 초기 설정된 재고량인 300개에 딱 맞춰 판매되었으며, 동시성 이슈로 인한 초과 판매(Overselling)가 전혀 발생하지 않았음을 입증합니다.

### 3. 최종 재고 상태 확인 (Inventory DB)
<img width="387" height="243" alt="스크린샷 2026-01-16 오후 5 20 56" src="https://github.com/user-attachments/assets/27bcea62-dceb-4f66-ba32-19253ad96505" />

* 테스트 종료 후 재고 테이블을 조회한 결과입니다. 판매된 수량(300개)만큼 정확하게 차감되어 모든 상품의 잔여 재고가 **0**이 되었습니다. 이를 통해 [주문 DB의 판매량]과 [재고 DB의 차감량]이 일치하는 데이터 무결성을 검증했습니다.

### 4. k6 부하 테스트 리포트
<img width="809" height="586" alt="스크린샷 2026-01-16 오후 5 20 24" src="https://github.com/user-attachments/assets/d9b0ea32-27a4-4216-8729-c82206379d06" />

* 5,000명(VUs)의 가상 사용자가 동시에 접속하여 총 20,000건의 요청을 보냈습니다.
* 결과는 에러율 0.00% (http_req_failed: 0.00%, checks_failed: 0.00%).
* 시스템이 셧다운되지 않고 2만 건의 요청을 안정적으로 받아냈습니다.
* 하지만 지연 시간이 11.3초입니다. 평균적으로 응답을 받기 위해 11초 정도 기다렸습니다.
* 병목이 생기는 이유 추측 : 서버의 스레드가 한정되어 있음(스레드가 부족), DB 커넥션 풀이 부족
* 추후 이를 개선하고 다시 부하 테스트 진행 예정

### 5. 톰캣 스레드, DB 커넥션 풀 튜닝 후 K6 부하 테스트(지연 시간 11.3초 -> 4.24초)

<img width="408" height="310" alt="스크린샷 2026-01-16 오후 7 21 04" src="https://github.com/user-attachments/assets/3f77326e-c3ab-4c82-99be-8ebcaceac4e8" />

* 톰캣 스레드(기본 200개)를 500개로 설정합니다.
* DB 커넥션 풀(기본 10개)을 20개로 설정합니다.

<img width="776" height="651" alt="스크린샷 2026-01-16 오후 7 22 17" src="https://github.com/user-attachments/assets/bb4a4c2c-d516-4601-bcfe-c19b2ac43431" />

톰캣 스레드와 DB 커넥션 풀을 튜닝하고 나서 다시 부하 테스트를 진행한 결과입니다.
* 처리량: 386 → 1,038 RPS (2.7배 증가)
* 지연 시간: 11.3s → 4.24초 (62% 개선)
* 안정성: 에러율 0% 유지

### 6. 동시성 제어 테스트 리포트

문제 발생: 하나의 상품(상품 id : 7)에 대해서 다수의 유저가 동시에 주문을 할 때 요청 모두가 성공하는 문제 발생 -> 테스트 시나리오를 만들고 해결해야 함.

테스트 시나리오: 재고가 단 1개 남은 특정 상품에 대해 100명(VUs)이 동시에 주문을 시도.

적용 기술: JPA Pessimistic Lock (비관적 락)

테스트 결과:

<img width="662" height="480" alt="스크린샷 2026-01-18 오전 1 09 01" src="https://github.com/user-attachments/assets/eca73ab6-51a2-4752-91b0-49cbe2aa4563" />

데이터 정합성: 100건의 요청 중 선착순 1건만 SUCCESS, 나머지 99건은 CANCELED 처리됨을 확인.

<img width="262" height="222" alt="스크린샷 2026-01-18 오전 1 12 59" src="https://github.com/user-attachments/assets/e5a48b58-5d07-4d1e-a554-8ddc2382cdea" />

재고 무결성: 재고가 마이너스가 되지 않고 정확히 0으로 종료됨.

발견된 문제점:

비관적 락은 DB 레벨에서 잠금을 걸기 때문에 요청이 몰릴 경우 앞선 트랜잭션이 끝날 때까지 다른 요청들이 대기하게 되어 응답 지연(Latency)이 길게 발생함.

---

### 1. Order Service API

#### 1) 주문 생성 (Create Order)
* **URL:** `/api/orders`
* **Method:** `POST`
* **Header:**  
  * `X-User-Id` (필수, 사용자 식별값)
* **설명:**  
  요청 헤더의 사용자 ID(`X-User-Id`)와 요청 본문의 주문 정보를 기반으로 주문을 생성합니다.   
  주문 생성에 성공하면 주문 ID와 총 금액을 반환합니다.
* **Request Body (JSON):**

  ```json
  {
    "items": [
      {
        "productId": 11,
        "productPrice": 2500,
        "quantity": 10
      },
      {
        "productId": 22,
        "productPrice": 55000,
        "quantity": 1
      },
      {
        "productId": 35,
        "productPrice": 12000,
        "quantity": 3
      }
    ]
  }
  ```
* **Response:**
  * **200 OK**: 주문 생성 성공
* **Response Body (JSON):**

  ```json
  {
    "orderId": 3,
    "totalPrice": 1362000,
    "orderStatus": "PENDING"
  }
  ```

#### 2) 주문 조회 (Read Order)
* **URL:** `/api/orders/{orderId}`
* **Method:** `GET`
* **Header:**  
  * `X-User-Id` (필수, 요청하는 사용자 식별값 — 본인 주문만 조회 가능)
* **Path Variables:**  
  * `orderId` (Long, 조회하고자 하는 주문의 고유 식별자)
* **설명:**  
  특정 주문(`orderId`)의 현재 상태와 상세 정보를 조회합니다.  
  주문은 **비동기 처리**로 진행되며, 주문 생성 직후 초기 상태는 `PENDING`입니다.  
  이후 재고 서비스(Inventory Service)가 Kafka 이벤트를 소비하여 재고 확인을 수행하고,  
  그 결과에 따라 주문 상태가 최종적으로 업데이트됩니다.
* **주문 상태(OrderStatus) 설명:**  
  * `PENDING`: 주문이 생성되었으나, 아직 재고 확인 및 차감이 완료되지 않은 대기 상태  
  * `SUCCESS`: 재고 서비스 확인 결과, 재고가 충분하여 주문이 정상적으로 확정된 상태  
  * `CANCELED`: 재고 서비스 확인 결과, 재고 부족 사유로 주문이 취소된 상태
* **Response:**  
  * **200 OK**: 주문 조회 성공
* **Response Body (JSON):**

  * **성공 시**
    ```json
    {
      "orderId": 101,
      "totalPrice": 55000,
      "orderStatus": "SUCCESS"
    }
    ```

  * **재고 부족으로 취소 시**
    ```json
    {
      "orderId": 102,
      "totalPrice": 120000,
      "orderStatus": "CANCELED"
    }
    ```

---

### 2. User Service API

#### 1) 회원가입 (Join)
* **URL:** `/user-service/join`
* **Method:** `POST`
* **설명:** `username` 중복 검사를 통과하면 비밀번호를 암호화하여 DB에 저장합니다. 현재 버전에서 모든 유저는 ADMIN 권한을 갖습니다.
* **Request Body (JSON):**

  ```json
  {
    "username" : "user1",
    "password" : "1234"
  }
  ```
  
#### 2) 로그인 (Login)
* **URL:** `/user-service/login`
* **Method:** `POST`
* **설명:** 아이디와 비밀번호가 일치하면 **JWT(Access Token)**를 생성하여 응답 헤더(Authorization)에 담아 반환합니다.
* **Request Body (JSON):**

  ```json
  {
    "username" : "user1",
    "password" : "1234"
  }
  ```
* **Response Header:**
  * `Authorization`: `Bearer {발급된 토큰값}`

#### 3) 관리자 페이지 접속 (Admin Access)
* **URL:** `/user-service/admin`
* **Method:** `GET`
* **설명:** 로그인 시 발급받은 JWT를 헤더에 포함하여 요청해야 합니다. (`ROLE_ADMIN` 권한 필요)
* **Request Header:**
  * `Authorization`: `Bearer {로그인 시 받은 JWT}`
* **Response:**
  * **200 OK**: 인증 및 인가 성공 시 "Admin Controller" 문자열 반환
  * **403 Forbidden**: 권한이 없거나 토큰이 유효하지 않을 경우

#### 4) 토큰 재발급 (Reissue)
* **URL:** `/user-service/reissue`
* **Method:** `POST`
* **설명:** Access Token이 만료되었을 때, 쿠키에 저장된 Refresh Token을 검증하여 새로운 Access Token을 발급받습니다. 새로운 발급받은 토큰의 Role은 ADMIN 권한이 아닌 USER 권한을 가집니다.(Refresh 토큰으로 재발급한 Access 토큰을 사용해 관리자 페이지(/user-service/admin) 접속 불가)
* **Request Cookie:**
  * `refresh`: `{로그인 시 발급받은 Refresh Token}`
* **Response Header:**
  * `Authorization`: `Bearer {새로 발급된 Access Token}`
* **Response:**
  * **200 OK**: 재발급 성공 (Header에 새 토큰 포함)
  * **400 Bad Request**: 쿠키가 없거나, Refresh Token이 유효하지 않음 (만료, DB 불일치)

---

### 3. Board Service API

**Base URL:** `/board-service/api/posts`
**공통 사항:** 모든 요청 헤더에 `Authorization: Bearer {Access_Token}`이 포함되어야 합니다. (Gateway에서 검증 후 유저 정보를 전달합니다.)

#### 1) 게시글 단건 조회 (Read Post)
* **URL:** `/api/posts/{id}`
* **Method:** `GET`
* **설명:** 게시글 ID(`id`)를 통해 상세 내용과 **해시태그 목록**을 조회합니다.
* **Path Variable:**
  - `id`: 게시글 고유 번호 (Long)
* **Response (JSON):**
  * **200 OK:** 조회 성공

  ```json
  {
    "id": 15,
    "author": "user1",
    "title": "테스트 제목",
    "content": "테스트 내용입니다.",
    "hashTags": [
      "Spring",
      "Java",
      "Backend"
    ]
  }
  ```
* **400 Bad Request:** 존재하지 않는 게시글 조회 시

#### 2) 게시글 작성 (Create Post)
* **URL:** `/` (Base URL)
* **Method:** `POST`
* **설명:** 새로운 게시글을 작성하고 해시태그를 등록합니다. 작성자(`author`)는 헤더의 `X-User-Name`에서 추출하여 저장됩니다.
* **Headers:**
  - `X-User-Name`: 작성자 아이디 (필수)
* **Request Body (JSON):**
  ```json
  {
    "title": "새로운 게시글 제목",
    "content": "게시글 본문 내용",
    "hashTags": [
      "Spring",
      "Tips"
    ]
  }
  ```
  * **Response (JSON):**
  * **200 OK:** 작성 성공 (저장된 게시글 정보 및 해시태그 반환)

    ```json
    {
      "id": 16,
      "author": "user1",
      "title": "새로운 게시글 제목",
      "content": "게시글 본문 내용",
      "hashTags": [
        "Spring",
        "Tips"
      ]
    }
    ```

#### 3) 게시글 수정 (Update Post)
* **URL:** `/api/posts/{id}`
* **Method:** `PUT`
* **설명:** 게시글의 제목, 내용, 해시태그를 수정합니다. 요청 헤더의 작성자(`X-User-Name`)와 DB의 게시글 작성자가 일치해야만 수정이 가능합니다. (기존 해시태그는 초기화되고 새로 입력한 목록으로 교체됩니다.)
* **Path Variable:**
  * `id`: 수정할 게시글 고유 번호
* **Headers:**
  - `X-User-Name`: 작성자 아이디 (필수)
* **Request Body (JSON):**
  ```json
  {
    "title": "수정된 제목",
    "content": "수정된 내용",
    "hashTags": [
      "Spring",
      "Update"
    ]
  }
  ```
  * **Response:**
  * **200 OK:** 수정 성공 (수정된 게시글 정보 및 해시태그 반환)
  
    ```json
    {
      "id": 16,
      "author": "user1",
      "title": "수정된 제목",
      "content": "수정된 내용",
      "hashTags": [
        "Spring",
        "Update"
      ]
    }
    ```
  * **403 Forbidden:** 본인이 작성하지 않은 글을 수정 시도 시

#### 4) 게시글 삭제 (Delete Post)
* **URL:** `/{id}`
* **Method:** `DELETE`
* **설명:** 게시글을 삭제합니다. **요청자(Token)**와 **게시글 작성자(DB)**가 일치해야만 삭제가 가능합니다.
* **Path Variable:**
  - `id`: 삭제할 게시글 고유 번호
* **Response:**
  * **200 OK:** 삭제 성공 (Body 없음)
  * **403 Forbidden:** 본인이 작성하지 않은 글을 삭제 시도 시


---

### 4. Board Service Comment API

**Base URL:** `/board-service/api`
**공통 사항:** 모든 요청 헤더에 `Authorization: Bearer {Access_Token}`이 포함되어야 합니다.

#### 1) 게시글별 댓글 목록 조회 (Read Comments by Post)
- **URL:** `/posts/{postId}/comments`
- **Method:** `GET`
- **설명:** 특정 게시글(`postId`)에 달린 댓글 목록을 페이징하여 조회합니다. (기본 50개씩 최신순)
- **Path Variable:**
  - `postId`: 게시글 고유 번호 (Long)
- **Query Parameter:**
  - `page`: 페이지 번호 (0부터 시작, 기본값 0)
- **Response:**
  - **200 OK:** 조회 성공
    ```json
    {
        "content": [
            {
                "id": 1,
                "content": "댓글 내용입니다.",
                "author": "user1"
            }
        ],
        "pageable": {
            "sort": {
                "empty": false,
                "sorted": true,
                "unsorted": false
            },
            "offset": 0,
            "pageNumber": 0,
            "pageSize": 50,
            "paged": true,
            "unpaged": false
        },
        "last": true,
        "totalPages": 1,
        "totalElements": 1,
        "size": 50,
        "number": 0,
        "sort": {
            "empty": false,
            "sorted": true,
            "unsorted": false
        },
        "first": true,
        "numberOfElements": 1,
        "empty": false
    }
    ```

#### 2) 댓글 작성 (Create Comment)
- **URL:** `/posts/{postId}/comments`
- **Method:** `POST`
- **설명:** 특정 게시글(`postId`)에 새로운 댓글을 작성합니다. 작성자(`author`)는 **헤더의 토큰**에서 추출하여 자동 저장됩니다.
- **Path Variable:**
  - `postId`: 게시글 고유 번호 (Long)
- **Request Body (JSON):**
  ```json
  {
    "content": "댓글 내용입니다."
  }
  ```
- **Response:**
  - **200 OK:** 작성 성공 (저장된 댓글 정보 반환)
    ```json
    {
      "id": 102,
      "content": "댓글 내용입니다.",
      "author": "user1"
    }
    ```
  - **400 Bad Request:** 존재하지 않는 게시글에 작성 시도 시

#### 3) 댓글 단건 조회 (Read Comment)
- **URL:** `/comments/{commentId}`
- **Method:** `GET`
- **설명:** 댓글 ID(`commentId`)를 통해 댓글 상세 내용을 조회합니다.
- **Path Variable:**
  - `commentId`: 댓글 고유 번호 (Long)
- **Response:**
  - **200 OK:** 조회 성공
    ```json
    {
      "id": 102,
      "content": "댓글 내용입니다.",
      "author": "user1"
    }
    ```
  - **400 Bad Request:** 존재하지 않는 댓글 조회 시

#### 4) 댓글 수정 (Update Comment)
- **URL:** `/comments/{commentId}`
- **Method:** `PUT`
- **설명:** 댓글을 수정합니다. **요청자(Token)**와 **댓글 작성자(DB)**가 일치해야만 수정이 가능합니다.
- **Path Variable:**
  - `commentId`: 수정할 댓글 고유 번호
- **Request Body (JSON):**
  ```json
  {
    "content": "수정된 댓글 내용"
  }
  ```
  - **Response:**
  - **200 OK:** 수정 성공 (수정된 댓글 정보 반환)
    ```json
    {
      "id": 102,
      "content": "수정된 댓글 내용",
      "author": "user1"
    }
    ```
  - **403 Forbidden:** 본인이 작성하지 않은 댓글을 수정 시도 시

#### 5) 댓글 삭제 (Delete Comment)
- **URL:** `/comments/{commentId}`
- **Method:** `DELETE`
- **설명:** 댓글을 삭제합니다. **요청자(Token)**와 **댓글 작성자(DB)**가 일치해야만 삭제가 가능합니다.
- **Path Variable:**
  - `commentId`: 삭제할 댓글 고유 번호
- **Response:**
  - **200 OK:** 삭제 성공 (Body 없음)
  - **403 Forbidden:** 본인이 작성하지 않은 댓글을 삭제 시도 시

---

### 5. Board Service API - Like

#### 1) 좋아요 등록 (Like)
* **URL:** `/api/posts/{id}/likes`
* **Method:** `POST`
* **설명:** 게시글에 좋아요를 등록합니다. MSA 환경이므로 Gateway에서 변환된 유저 ID(`X-User-Id`)를 헤더로 받아 처리합니다. 이미 좋아요를 누른 경우 예외가 발생합니다.
* **Request Header:**
  * `X-User-Id`: `{User PK (Long)}` (Gateway에서 JWT 파싱 후 전달)
* **Path Variable:**
  * `id`: 좋아요를 누를 게시글의 ID (Post ID)
* **Request Body:**
  * `None` (Body는 비워둠)
* **Response:**
  * **200 OK**: 등록 성공
  * **400 Bad Request**: 이미 좋아요를 누른 상태일 경우 ("이미 좋아요를 눌렀습니다.")
* **Response Body (JSON):**

  ```json
  {
    "postId": 1,
    "memberId": 123
  }
  ```

#### 2) 좋아요 취소 (Unlike)
* **URL:** `/api/posts/{id}/likes`
* **Method:** `DELETE`
* **설명:** 등록된 좋아요를 취소합니다. 좋아요 기록이 없는 경우 예외를 반환합니다.
* **Request Header:**
  * `X-User-Id`: `{User PK (Long)}`
* **Path Variable:**
  * `id`: 좋아요를 취소할 게시글의 ID (Post ID)
* **Response:**
  * **200 OK**: 취소 성공
  * **403 Forbidden**: 좋아요를 누른 적이 없는 경우 ("좋아요를 누른 적이 없습니다.")
* **Response Body (JSON):**

  ```json
  {
    "postId": 1,
    "memberId": 123
  }
  ```

---

## Minikube 설정

본 프로젝트는 **Minikube** 클러스터 환경에서 운영되며 Nginx 설정 및 보안 환경 변수를 효율적으로 관리하기 위해 쿠버네티스의 ConfigMap과 Secret을 사용합니다.

### 1. Nginx 설정용 ConfigMap 생성
Reverse Proxy 역할을 하는 Nginx의 설정 파일(`nginx.conf`)을 관리합니다.

```bash
kubectl create configmap nginx-config --from-file=nginx.conf
```

### 2. 보안 환경 변수 관리 (Secret)
JWT Secret Key는 .env 파일에 저장하고 이를 쿠버네티스 Secret으로 변환해 컨테이너에 주입합니다.

```bash
kubectl create secret generic lucky-secret --from-env-file=.env
```
