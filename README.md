# E-Commerce REST API

A production-grade e-commerce backend built with **Spring Boot 3**, featuring JWT authentication, device session management, Redis caching, Razorpay payment integration, and automated email notifications.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security + JWT (JJWT) |
| Database | MySQL 8 + Spring Data JPA |
| Cache | Redis |
| Email | Spring Mail (Gmail SMTP) |
| Payments | Razorpay |
| Docs | Swagger / OpenAPI 3 |
| Build | Maven |
| Container | Docker + Docker Compose |

---

## Architecture

```
src/main/java/com/ecommerce/demo/
├── config/           # SecurityConfig, RedisConfig
├── controller/       # UserController, AdminBasedController,
│                     # UserAccessController, ForgetPasswordController
├── dto/              # Request/Response DTOs
├── email/            # EmailServices, MailBody, ForgetPassword
├── exception/        # Custom exceptions + GlobalExceptionHandler
├── model/            # JPA Entities
├── payment/          # PaymentService, PaymentController (Razorpay)
├── pagination/       # ProductSpecification (JPA filtering)
├── repository/       # Spring Data JPA repositories
├── security/         # JwtUtils, JwtAuthFilter
└── service/          # Business logic
```

---

## Running Locally

### Prerequisites
- Docker & Docker Compose installed
- Gmail account with App Password enabled

### 1. Set environment variables

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
# Fill in your DB_PASSWORD, MAIL_USERNAME, MAIL_PASSWORD, JWT_SECRET
```

### 2. Start everything with Docker

```bash
docker-compose up --build
```

This starts MySQL, Redis, and the Spring Boot app. API is available at `http://localhost:8080`.

### 3. View API docs

Open `http://localhost:8080/swagger-ui/index.html`

---

## API Overview

### Authentication (no token needed)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/users/register` | Register new user |
| POST | `/api/users/login` | Login → returns accessToken + refreshToken |
| POST | `/api/users/refresh` | Get new accessToken using refreshToken |
| POST | `/api/forgetPassword/verifyEmail/{email}` | Send OTP to email |
| POST | `/api/forgetPassword/verifyOtp/{otp}/{email}` | Verify OTP |
| POST | `/api/forgetPassword/changePassword/{email}/{otp}` | Reset password |

### User endpoints (requires `Authorization: Bearer <accessToken>`)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/user/getAllProducts` | List all products |
| GET | `/api/user/filter` | Filter products by category, price, name |
| POST | `/api/user/createOrder` | Place an order |
| GET | `/api/user/getAllOrdersByEmail/{email}` | Get my orders |
| POST | `/api/user/payment/create?orderId=1` | Initiate Razorpay payment |
| POST | `/api/user/payment/verify` | Verify payment signature |
| GET | `/api/user/myCartItems` | View cart |
| POST | `/api/user/addcartItems` | Add to cart |
| POST | `/api/user/createReview` | Leave a review |

### Admin endpoints (requires ROLE_ADMIN)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/admin/createProduct` | Add product |
| PUT | `/api/admin/updateProduct/{id}` | Update product |
| DELETE | `/api/admin/deleteProduct/{id}` | Delete product |
| PUT | `/api/admin/updateOrderStatus/{id}` | Update order status |
| GET | `/api/admin/getAllReviews` | View all reviews |

---

## Frontend (AngularJS 1.8)

A single-page storefront + admin panel built with **AngularJS only** (angular, angular-route, angular-messages — vendored locally, no CDN, no CSS framework, no build step).

It lives in `src/main/resources/static/`, so Spring Boot serves it automatically:

```bash
docker-compose up --build
# open http://localhost:8080
```

Prefer to run the UI separately (e.g. VS Code Live Server on :5500)? Set `apiBase: 'http://localhost:8080'` in `static/app/core/config.js`. CORS is enabled for `localhost` origins.

### Screens

| Who | Screens |
|---|---|
| Everyone | Sign in, create account, forgot password (email → 6-digit OTP with countdown → new password) |
| Customer (`ROLE_USER`) | Shop (search, category, price range, sort, paging via `/filter`), product page (size/qty, reviews), bag (±qty, change size, remove, checkout), payment (Razorpay), orders (pay / cancel), my reviews |
| Admin (`ROLE_ADMIN`) | Overview (stock & review stats, low-stock list), products (create / edit stock / delete), orders (find by customer email or order number, move status), reviews |
| Both | Account: profile, signed-in devices (2-device limit, remove device), delete account |

### Validation

Every form validates on the client with `ngMessages`, mirroring (or tightening) the backend's rules, and shows backend errors on the right field:

| Form | Rules |
|---|---|
| Register | name 3–50, starts with a letter; real email (`a@b.co`, not `a@b`); password ≥ 6 with a letter + a digit, no spaces, strength meter; confirm must match; terms required; duplicate email shown on the email field (HTTP 409) |
| Sign in | email format, password required; `400 {fields}` mapped onto inputs |
| Forgot password | email exists (404 → field error); OTP exactly 6 digits; wrong OTP → field error; passwords match |
| Shop filters | min price ≤ max price, non-negative; search ≤ 60 chars |
| Add to bag | size required; whole-number quantity 1…stock (max 10) |
| Review | 1–5 stars required; comment 10–500 chars, not just spaces |
| Checkout | each line re-checked against live stock; confirmation required |
| Product (admin) | name 2–100; description 10–255; price ₹1–₹10,00,000 with ≤ 2 decimals; category letters only; at least one size; stock whole number 0–10,000; duplicate name+description blocked |
| Order lookup (admin) | valid email / positive whole order number; status changes limited to the allowed flow (PENDING → PAID/CANCELLED → SHIPPED → DELIVERED) |
| Delete account | password required (wrong password → field error); type `DELETE` |

Reusable validator directives are in `static/app/validation/validators.js` (`tl-email`, `tl-match`, `tl-strong-password`, `tl-otp`, `tl-integer`, `tl-decimals`, `tl-not-greater-than`, `tl-valid-if`, …).

### Auth handling

* Tokens are kept in `localStorage`; every API call gets `Authorization: Bearer …` from an `$http` interceptor.
* The access token (15 min) is refreshed automatically just before expiry, and a 401 triggers one refresh + retry. If the refresh token is also expired, the user is sent to sign in and returned to the page they were on.
* Routes are guarded by role; admins and customers each land on their own home screen.

### Backend changes made for the frontend

Search the code for `FRONTEND` to see each one.

| Change | Why |
|---|---|
| `GET /api/users/me` → `{id, username, email, roles}` (`CurrentUserDTO`) | The JWT only carries the email, but cart / order / review requests need the user id, and the UI needs the role |
| `id` added to `ProductDTO`; `id` + `productId` to `CartItemResponseDTO`; `id` + `userId` to `UserOrderResponse` | Without ids the UI can't open a product, remove a cart line, pay for / cancel an order, or update order status |
| `SecurityConfig`: static files permitted, CORS for localhost, 401 (not 403) for missing/expired tokens | Serve the UI; let the UI tell "expired token" from "wrong role" |
| `ProductService.addProduct` links each size to its product | `ProductSize.product` is `@JsonIgnore`, so sizes posted as JSON were saved with `product_id = NULL` and could never be added to a cart |
| `UserService.registerNewUserAccount` defaults `roles` to `ROLE_USER` | A user saved without roles caused a NullPointerException on login |
| `/register` returns **409** for a duplicate email | Previously returned 201 "Email already registered" |
| `OrderService.createOrder` sets `orderDate` + `status = PENDING`; `ReviewService.createReview` sets `reviewDate` | Server-side values instead of trusting the client (a client could post `"status": "PAID"`) |

### Things to know

* **Admin self-registration:** the backend accepts `roles: ROLE_ADMIN` from `/register`, so the sign-up form offers "Store admin" for testing. Before going live, remove that option and the `ROLE_ADMIN` branch in `UserService.registerNewUserAccount`, and create admins directly in the database.
* **Payments:** `PaymentService` is still a stub (`order_stub_…`). The payment page detects this and explains it; once the Razorpay SDK lines are enabled and real test keys are set, it opens Razorpay Checkout and calls `/payment/verify` automatically.
* **No "all orders" endpoint:** admins look orders up by customer email or order number.
* **Editing products:** `updateProduct` can change stock for existing sizes only, so sizes are locked when editing.

---

## Key Features

**JWT Authentication** — Stateless access + refresh token flow. Access tokens expire in 15 minutes; refresh tokens in 7 days.

**Device Session Restriction** — Each account is limited to 2 active devices simultaneously (configurable). Login, logout, and remote device removal supported.

**OTP-Based Password Reset** — 6-digit OTP sent via Gmail SMTP, expires in ~12 minutes.

**Redis Caching** — Product lookups cached for 10 minutes. Cache automatically invalidated on update/delete.

**Razorpay Payments** — Full payment flow: initiate → frontend collects payment → verify HMAC signature → mark order as PAID.

**Email Notifications** — Automatic emails on: order placed, order shipped, order delivered.

**Centralized Error Handling** — All exceptions handled by a single `GlobalExceptionHandler` returning consistent JSON error responses.

---

## Running Tests

```bash
mvn test
```

Tests cover: UserService (registration, login, device management), ProductService (CRUD, cache eviction), JwtUtils (token generation and validation).

---

## What I'd improve next

- Add Elasticsearch for full-text product search
- Add an address/shipping model
- Add admin analytics endpoints (revenue by date, top products)
- Deploy to AWS EC2 with RDS and ElastiCache
