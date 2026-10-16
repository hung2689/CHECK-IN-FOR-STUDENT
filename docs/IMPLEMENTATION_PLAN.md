# QR Attendance System - Implementation Plan

## Current Project Status
- Empty directory, building from scratch
- Environment: Java 21, Maven 3.9.15, Node 24.13, npm 11.6.2, Windows 11

## Proposed Architecture

```
checkin/
├── backend/          # Spring Boot 3.5.x + Maven
│   └── src/
│       ├── main/
│       │   ├── java/com/example/attendance/
│       │   │   ├── config/
│       │   │   ├── security/
│       │   │   ├── auth/
│       │   │   ├── classmanagement/
│       │   │   ├── student/
│       │   │   ├── attendance/
│       │   │   ├── report/
│       │   │   ├── common/
│       │   │   └── exception/
│       │   └── resources/
│       │       └── db/migration/
│       └── test/
├── frontend/         # React + Vite + TypeScript + Tailwind
│   └── src/
│       ├── components/
│       ├── pages/
│       ├── services/
│       ├── hooks/
│       └── types/
├── docs/
└── vercel.json
```

## Database Schema

### Tables
1. **users** - TA accounts with BCrypt passwords
2. **classes** - Class management with unique codes
3. **students** - Student records with codes and names
4. **class_students** - Many-to-many enrollment (unique class_id + student_id)
5. **attendance_sessions** - Sessions with check-in/check-out stages
6. **attendance_tokens** - Secure random tokens for QR codes
7. **attendance_records** - Attendance data (unique session_id + student_id)

## API Endpoints
See docs/API.md for complete API contract.

## Implementation Phases
1. Backend scaffold + database migrations
2. Authentication (Spring Security + sessions)
3. Class & student management + Excel import
4. Attendance sessions + QR token generation
5. Public attendance submission
6. Dashboard + Excel export
7. Frontend: auth, class mgmt, student mgmt
8. Frontend: sessions, QR display, public attendance
9. Frontend: dashboard, export
10. Testing + deployment config

## Deployment
- Frontend → Vercel (SPA with rewrites)
- Backend → Render (Web Service, Java)
- Database → Neon PostgreSQL

## Known Risks
- Students can share QR codes / student codes (documented limitation)
- Network latency on free Render tier may affect cold starts
- Neon free tier has connection limits (configure pool accordingly)
