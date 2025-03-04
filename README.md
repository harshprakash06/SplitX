# SplitX - Expense Sharing System

Welcome to **SplitX**, a powerful expense-sharing and tracking system built using **Spring Boot**. 🚀

## 📌 Overview
SplitX simplifies group expense management, allowing users to split bills, track payments, and settle debts efficiently. It also features a **real-time chat system** for seamless communication between users.

## 🛠 Tech Stack
### Backend:
- **Spring Boot** (Java)
- **Spring Security** (Authentication & Authorization)
- **Spring Data JPA** (Database Management)
- **MySQL & MongoDB** (Databases)
- **Custom Token System** (Cipher-based authentication)
- **WebSockets** (Real-time messaging)

### Other Tools:
- **Postman** (API Testing & Overview)

## 🚀 Features
- **User Authentication & Authorization** (Custom Cipher-based Token System)
- **Create & Manage Groups**
- **Add Expenses & Split Bills**
- **Track Payments & Settlements**
- **Real-time Chat System** (WebSockets)
- **Secure API Endpoints**

## 📂 Project Setup
### Backend (Spring Boot)
#### 1️⃣ Clone the Repository
```sh
 git clone https://github.com/your-repo/splitx.git
 cd splitx
```

#### 2️⃣ Configure Databases (MySQL & MongoDB)
Update `application.properties` in `src/main/resources/`:
```properties
# MySQL Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/splitx_db
spring.datasource.username=root
spring.datasource.password=yourpassword

# MongoDB Configuration
spring.data.mongodb.uri=mongodb://localhost:27017/splitx_db
```

#### 3️⃣ Build & Run the Backend
```sh
 mvn clean install
 mvn spring-boot:run
```
Backend will run at `http://localhost:8080/`

## 📜 API Documentation
### 🔹 Postman API Collection
[View API Documentation on Postman](your-postman-link-here)

## 🔧 Build for Production
To create an optimized production build:
```sh
 mvn package
```

## 📜 License
This project is licensed under the [MIT License](LICENSE).

## 📬 Contact
For any queries or collaborations, reach out to us at **[your email/contact info]**.

---
Developed with ❤️ by the **SplitX Team**
