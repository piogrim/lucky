# lucky
=======

## 11-30 아키텍처 및 API 명세

### 1. 아키텍처 흐름
모든 요청은 **Nginx**를 통해 들어와 **Gateway**를 거쳐 **User Service**로 전달됩니다.

1. **Nginx (Port 80)**
   - 모든 외부 HTTP 요청을 수신
   - **lucky-gateway**로 요청을 전달

2. **lucky-gateway(Spring Cloud Gateway)**
   - 요청 경로를 분석하여 특정 서비스로 라우팅
   - `/user-service/**` 경로의 요청을 **user-service**로 전달

3. **user-service**
   - 회원가입, 로그인, JWT 발급 및 인증/인가 로직 수행

---

### 2. User Service API

#### 1) 회원가입 (Join)
* **URL:** `/user-service/join`
* **Method:** `POST`
* **설명:** `username` 중복 검사를 통과하면 비밀번호를 암호화하여 DB에 저장합니다. 현재 버전에서 모든 유저는 ADMIN 권한을 갖습니다.
* **Body (JSON):**
  ```json
  {
    "username" : "user1",
    "password" : "1234"
  }
  
#### 2) 로그인 (Login)
* **URL:** `/user-service/login`
* **Method:** `POST`
* **설명:** 아이디와 비밀번호가 일치하면 **JWT(Access Token)**를 생성하여 응답 헤더(Authorization)에 담아 반환합니다.
* **Body (JSON):**
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