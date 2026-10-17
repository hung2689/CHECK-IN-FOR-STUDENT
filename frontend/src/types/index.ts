export interface User {
  id: number;
  username: string;
  displayName?: string;
  name?: string;
  role: string;
}

export interface ClassItem {
  id: number;
  classCode: string;
  name: string;
  description?: string;
  studentCount?: number;
  active?: boolean;
  createdAt: string;
}

export interface Student {
  id: number;
  studentCode: string;
  fullName: string;
  email?: string;
}

export interface ImportError {
  row: number;
  studentCode: string;
  message: string;
}

export interface ImportResult {
  totalRows: number;
  successCount: number;
  skippedCount: number;
  errorCount: number;
  errors: ImportError[];
}

export type SessionStatus = 'PENDING' | 'CHECK_IN_OPEN' | 'CHECK_IN_CLOSED' | 'CHECK_OUT_OPEN' | 'COMPLETED' | 'CANCELLED';
export type StageStatus = 'NOT_STARTED' | 'OPEN' | 'CLOSED';

export interface AttendanceSession {
  id: number;
  classId: number;
  className: string;
  classCode: string;
  name: string;
  description?: string;
  attendanceDate: string;
  status: SessionStatus;
  checkInStatus: StageStatus;
  checkOutStatus: StageStatus;
  checkInToken?: string;
  checkOutToken?: string;
  totalStudents: number;
  checkedInCount: number;
  checkedOutCount: number;
  createdAt: string;
  geofenceLat?: number;
  geofenceLng?: number;
  geofenceRadiusM?: number;
  gpsEnforcement?: boolean;
  pendingReviewCount?: number;
}

export interface AttendanceRecord {
  id?: number;
  studentId: number;
  studentCode: string;
  fullName: string;
  checkInAt?: string;
  checkOutAt?: string;
  attendanceStatus?: string;
}

export interface PublicTokenInfo {
  valid: boolean;
  className?: string;
  sessionName?: string;
  stage?: string;
  attendanceDate?: string;
  message?: string;
}

export interface PublicAttendanceResponse {
  success: boolean;
  studentName?: string;
  stage?: string;
  gpsResult?: string;
  reviewRequired?: boolean;
  message: string;
}

export interface AttendanceReviewAttempt {
  id: number;
  sessionId: number;
  sessionName?: string;
  studentId: number;
  studentCode: string;
  studentName: string;
  stage: 'CHECK_IN' | 'CHECK_OUT';
  submittedAt: string;
  latitude?: number;
  longitude?: number;
  gpsAccuracyM?: number;
  distanceFromSchool?: number;
  gpsResult?: 'VERIFIED' | 'OUTSIDE_GEOFENCE' | 'POOR_ACCURACY' | 'BOUNDARY' | 'NO_LOCATION' | 'DISABLED';
  gpsReason?: string;
  reviewStatus: 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'REVIEWED';
  reviewedBy?: number;
  reviewerUsername?: string;
  reviewedAt?: string;
  adminDecision?: 'APPROVED' | 'REJECTED';
  adminNote?: string;
}

export interface ReviewStats {
  totalStudents: number;
  confirmedCheckIns: number;
  confirmedCheckOuts: number;
  pendingReviews: number;
  rejectedAttempts: number;
  suspiciousAttempts: number;
}
