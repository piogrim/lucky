# lucky
=======

아키텍처 다이어그램

<img width="1028" height="540" alt="스크린샷 2025-12-05 오전 4 27 52" src="https://github.com/user-attachments/assets/749b3441-39ed-49dc-90cb-e257e874e5a7" />



## 2025.12.05 아키텍처 및 API 명세

### 1. 아키텍처 흐름
모든 요청은 **Nginx**를 통해 들어와 **Gateway**를 거쳐 **User Service**, **Board Service**로 전달됩니다.

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

### 2. Board Service API

**Base URL:** `/board-service/api/posts`
**공통 사항:** 모든 요청 헤더에 `Authorization: Bearer {Access_Token}`이 포함되어야 합니다. (Gateway에서 검증 후 유저 정보를 전달합니다.)

#### 1) 게시글 단건 조회 (Read Post)
* **URL:** `/{id}`
* **Method:** `GET`
* **설명:** 게시글 ID(`id`)를 통해 상세 내용을 조회합니다.
* **Path Variable:**
  - `id`: 게시글 고유 번호 (Long)
* **Response (JSON):**
  * **200 OK:** 조회 성공

  ```json
  {
    "id": 15,
    "author": "user1",
    "title": "테스트 제목",
    "content": "테스트 내용입니다."
  }
  ```
  * **400 Bad Request:** 존재하지 않는 게시글 조회 시

#### 2) 게시글 작성 (Create Post)
* **URL:** `/` (Base URL)
* **Method:** `POST`
* **설명:** 새로운 게시글을 작성합니다. 작성자(`author`)는 **헤더의 토큰**에서 추출하여 자동 저장됩니다.
* **Request Body (JSON):**

  ```json
  {
    "title": "새로운 게시글 제목",
    "content": "게시글 본문 내용"
  }
  ```
  * **Response (JSON):**
  * **200 OK:** 작성 성공 (저장된 게시글 정보 반환)

    ```json
    {
      "id": 16,
      "author": "user1",
      "title": "새로운 게시글 제목",
      "content": "게시글 본문 내용"
    }
    ```

#### 3) 게시글 수정 (Update Post)
* **URL:** `/{id}`
* **Method:** `PUT`
* **설명:** 게시글을 수정합니다. **요청자(Token)**와 **게시글 작성자(DB)**가 일치해야만 수정이 가능합니다.
* **Path Variable:**
  * `id`: 수정할 게시글 고유 번호
* **Request Body (JSON):**

  ```json
  {
    "title": "수정된 제목",
    "content": "수정된 내용"
  }
  ```
  * **Response:**
  * **200 OK:** 수정 성공 (수정된 게시글 정보 반환)
  
    ```json
    {
      "id": 16,
      "author": "user1",
      "title": "수정된 제목",
      "content": "수정된 내용"
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
