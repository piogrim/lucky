# lucky
=======

아키텍처 다이어그램

<img width="1093" height="656" alt="스크린샷 2025-12-29 오전 5 55 39" src="https://github.com/user-attachments/assets/45827d5f-f4b9-4c48-b270-34491caeed89" />
일반적으로 쿠버네티스에서 컨테이너 런타임으로 containerd를 사용하나 본 프로젝트는 minikube를 이용하고 컨테이너 런타임으로 docker를 사용했습니다.

## 2025.12.29 아키텍처 및 API 명세

### 1. 아키텍처 흐름
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
* **설명:** 새로운 게시글을 작성하고 해시태그를 등록합니다. 작성자(`author`)는 헤더의 `X-User-Name`에서 추출하여 자동 저장됩니다.
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
