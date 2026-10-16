# QR Attendance System - Hệ Thống Điểm Danh QR Cho Lớp Học

Hệ thống điểm danh bằng mã QR hoàn chỉnh dành cho Trợ giảng (TA) và Giảng viên để quản lý lớp học khoảng 50 sinh viên, hỗ trợ quét mã 2 giai đoạn (Check-in đầu giờ & Check-out cuối giờ), chống gian lận, nhập danh sách từ Excel và xuất báo cáo kết quả điểm danh.

---

## 🛠️ Công Nghệ Sử Dụng

### Backend
- **Java 21** & **Spring Boot 3.5.x**
- **Spring Security** (Session-based Authentication, BCrypt Password Encryption, CORS)
- **Spring Data JPA & Hibernate**
- **Flyway Migrations** (Schema versioning & indexing)
- **PostgreSQL** (Hỗ trợ PostgreSQL cục bộ hoặc Neon Cloud Database)
- **Apache POI** (Đọc/ghi file Excel .xlsx hiệu năng cao)

### Frontend
- **React 19** + **TypeScript**
- **Vite 8**
- **Tailwind CSS v4** + Glassmorphism UI
- **React Router v7**
- **Axios** (WithCredentials, interceptors)
- **qrcode.react** (Tạo SVG QR code sắc nét ở chế độ Fullscreen máy chiếu)
- **Lucide React** (Bộ icon hiện đại)

---

## 🚀 Tính Năng Chính

1. **Quản Lý Lớp Học**:
   - Thêm mới lớp, cấu hình mã lớp, học kỳ, ghi chú.
   - Quản lý danh sách sĩ số ~50 sinh viên.

2. **Nhập & Xuất Excel**:
   - Tải file mẫu danh sách sinh viên chuẩn `.xlsx`.
   - Nhập danh sách sinh viên hàng loạt từ file Excel (`sample_students_50.xlsx` được tạo sẵn kèm 50 sinh viên).
   - Tự động bỏ qua trùng lặp hoặc báo cáo lỗi dòng cụ thể.

3. **Buổi Điểm Danh QR Hai Giai Đoạn**:
   - **Giai đoạn 1 - Check-in (Đầu giờ)**: Tạo mã QR động để sinh viên quét nhận diện có mặt.
   - **Giai đoạn 2 - Check-out (Cuối giờ)**: Đổi mã token mới để xác nhận sinh viên ở lại đến hết buổi học.
   - Chế độ **Toàn Màn Hình (Fullscreen)** tối ưu chiếu lên máy chiếu giảng đường.

4. **Bảng Theo Dõi Thời Gian Thực (Live Roster Room)**:
   - Tự động cập nhật danh sách sinh viên đã điểm danh (real-time polling 3.5s).
   - Hiển thị tỷ lệ có mặt, số lượng đã check-in, check-out, vắng mặt.
   - Xuất bảng tổng kết điểm danh chi tiết ra file Excel.

5. **Trang Quét Mã Dành Cho Sinh Viên (`/checkin/:token`)**:
   - Giao diện thân thiện trên điện thoại di động.
   - Nhập Mã số sinh viên (MSSV) để xác nhận.
   - Ghi nhớ MSSV trong bộ nhớ điện thoại (localStorage) để sinh viên không phải gõ lại lúc check-out.

---

## 🔑 Tài Khoản Mặc Định

- **Username**: `admin`
- **Password**: `admin123`
*(Tự động khởi tạo khi ứng dụng chạy lần đầu)*

---

## 💻 Hướng Dẫn Chạy Cục Bộ (Local Development)

### 1. Chuẩn bị Cơ sở dữ liệu PostgreSQL
Tạo cơ sở dữ liệu có tên `attendance` trong PostgreSQL:
```sql
CREATE DATABASE attendance;
```

Cấu hình thông tin kết nối trong [backend/src/main/resources/application.yml](file:///d:/TRO_GIANG/checkin/backend/src/main/resources/application.yml):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/attendance
    username: postgres
    password: postgres
```

### 2. Chạy Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run
```
Backend sẽ khởi chạy tại: `http://localhost:8080`.

### 3. Chạy Frontend (React Vite)
```bash
cd frontend
npm install
npm run dev
```
Frontend sẽ khởi chạy tại: `http://localhost:3000` (hoặc `http://localhost:5173`).

---

## 🌐 Hướng Dẫn Deploy Online "Tonight"

### 1. Cơ sở dữ liệu (Neon Postgres - Miễn phí)
1. Đăng ký tại [neon.tech](https://neon.tech) và tạo 1 Project Postgres.
2. Sao chép chuỗi kết nối (`postgresql://username:password@ep-xyz.neon.tech/neondb?sslmode=require`).

### 2. Backend (Render / Railway)
1. Tạo một **Web Service** mới trên [render.com](https://render.com) liên kết với Git repository này.
2. Root Directory: `backend`
3. Environment: `Docker` (Sử dụng `backend/Dockerfile` đã cấu hình sẵn) hoặc `Java`.
4. Cấu hình biến môi trường (Environment Variables):
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `DATABASE_URL`: `jdbc:postgresql://<neon-host>/neondb?sslmode=require`
   - `DB_USERNAME`: `<neon-username>`
   - `DB_PASSWORD`: `<neon-password>`
   - `FRONTEND_ORIGIN`: `https://your-frontend.vercel.app`

### 3. Frontend (Vercel)
1. Import repository lên [vercel.com](https://vercel.com).
2. Root Directory: `frontend`
3. Framework Preset: `Vite`
4. Biến môi trường (Environment Variables):
   - `VITE_API_URL`: `https://your-backend.onrender.com`
5. Bấm **Deploy**. Vercel sẽ tự động cấu hình SPA rewrite dựa trên `vercel.json`.

---

## 📁 Cấu Trúc Thư Mục Dự Án

```
checkin/
├── backend/                       # Spring Boot 3 Backend
│   ├── src/main/java/com/example/attendance/
│   │   ├── auth/                  # Quản lý TA và đăng nhập
│   │   ├── classmanagement/       # Quản lý lớp học
│   │   ├── student/               # Sinh viên & Import Excel
│   │   ├── attendance/            # Buổi điểm danh, QR token & Public API
│   │   ├── report/                # Xuất báo cáo Excel
│   │   ├── config/                # Security, CORS & Bootstrap
│   │   └── exception/             # Xử lý lỗi toàn cục
│   ├── src/main/resources/
│   │   └── db/migration/          # Flyway SQL migrations
│   └── Dockerfile                 # Multi-stage container build
├── frontend/                      # React 19 + TypeScript + Vite + Tailwind v4
│   ├── src/
│   │   ├── components/            # Navbar, ProtectedRoute
│   │   ├── pages/                 # Login, Classes, ClassDetail, SessionLive, PublicCheckin
│   │   ├── context/               # AuthContext
│   │   ├── services/              # Axios API client
│   │   └── types/                 # TypeScript interfaces
│   ├── vercel.json                # SPA rewrite rules
│   └── vite.config.ts             # Tailwind v4 & Vite config
├── docs/                          # Tài liệu kiến trúc và kế hoạch
├── sample_students_50.xlsx        # File mẫu 50 sinh viên để test import ngay lập tức
└── vercel.json                    # Root deployment config
```
