package com.example.attendance.report;

import com.example.attendance.attendance.*;
import com.example.attendance.classmanagement.ClassEntity;
import com.example.attendance.classmanagement.ClassRepository;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.student.Student;
import com.example.attendance.student.StudentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final AttendanceReviewAttemptRepository reviewAttemptRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;

    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    public ReportService(AttendanceSessionRepository sessionRepository,
                         AttendanceRecordRepository recordRepository,
                         AttendanceReviewAttemptRepository reviewAttemptRepository,
                         ClassRepository classRepository,
                         StudentRepository studentRepository) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.reviewAttemptRepository = reviewAttemptRepository;
        this.classRepository = classRepository;
        this.studentRepository = studentRepository;
    }

    public byte[] exportAttendance(Long sessionId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        ClassEntity classEntity = classRepository.findById(session.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        List<Student> students = studentRepository.findStudentsByClassId(session.getClassId());
        List<AttendanceRecord> records = recordRepository.findBySessionId(sessionId);
        List<AttendanceReviewAttempt> attempts = reviewAttemptRepository.findBySessionIdOrderBySubmittedAtDesc(sessionId);

        Map<Long, AttendanceRecord> recordMap = new HashMap<>();
        for (AttendanceRecord r : records) {
            recordMap.put(r.getStudentId(), r);
        }

        Map<Long, List<AttendanceReviewAttempt>> attemptMap = new HashMap<>();
        for (AttendanceReviewAttempt a : attempts) {
            attemptMap.computeIfAbsent(a.getStudentId(), k -> new ArrayList<>()).add(a);
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Attendance");

            // Styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle infoStyle = workbook.createCellStyle();
            Font infoFont = workbook.createFont();
            infoFont.setFontHeightInPoints((short) 11);
            infoStyle.setFont(infoFont);

            // 1. ABSENT: Vắng mặt (Màu đỏ hồng)
            CellStyle absentStyle = workbook.createCellStyle();
            Font absentFont = workbook.createFont();
            absentFont.setBold(true);
            absentFont.setColor(IndexedColors.DARK_RED.getIndex());
            absentStyle.setFont(absentFont);
            absentStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            absentStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            absentStyle.setAlignment(HorizontalAlignment.CENTER);
            absentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            absentStyle.setBorderBottom(BorderStyle.THIN);
            absentStyle.setBorderTop(BorderStyle.THIN);
            absentStyle.setBorderLeft(BorderStyle.THIN);
            absentStyle.setBorderRight(BorderStyle.THIN);

            // 2. MISSING CHECK-IN / CHECK-OUT: Thiếu check-in hoặc check-out (Màu vàng nhạt)
            CellStyle warningStyle = workbook.createCellStyle();
            Font warningFont = workbook.createFont();
            warningFont.setBold(true);
            warningFont.setColor(IndexedColors.DARK_YELLOW.getIndex());
            warningStyle.setFont(warningFont);
            warningStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            warningStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            warningStyle.setAlignment(HorizontalAlignment.CENTER);
            warningStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            warningStyle.setBorderBottom(BorderStyle.THIN);
            warningStyle.setBorderTop(BorderStyle.THIN);
            warningStyle.setBorderLeft(BorderStyle.THIN);
            warningStyle.setBorderRight(BorderStyle.THIN);

            // 3. PRESENT: Có mặt đầy đủ (Màu xanh lá)
            CellStyle presentStyle = workbook.createCellStyle();
            Font presentFont = workbook.createFont();
            presentFont.setBold(true);
            presentFont.setColor(IndexedColors.DARK_GREEN.getIndex());
            presentStyle.setFont(presentFont);
            presentStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            presentStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            presentStyle.setAlignment(HorizontalAlignment.CENTER);
            presentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            presentStyle.setBorderBottom(BorderStyle.THIN);
            presentStyle.setBorderTop(BorderStyle.THIN);
            presentStyle.setBorderLeft(BorderStyle.THIN);
            presentStyle.setBorderRight(BorderStyle.THIN);

            // 4. PENDING_REVIEW: Đang chờ duyệt (Màu cam đào)
            CellStyle pendingStyle = workbook.createCellStyle();
            Font pendingFont = workbook.createFont();
            pendingFont.setBold(true);
            pendingFont.setColor(IndexedColors.DARK_RED.getIndex());
            pendingStyle.setFont(pendingFont);
            pendingStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            pendingStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            pendingStyle.setAlignment(HorizontalAlignment.CENTER);
            pendingStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            pendingStyle.setBorderBottom(BorderStyle.THIN);
            pendingStyle.setBorderTop(BorderStyle.THIN);
            pendingStyle.setBorderLeft(BorderStyle.THIN);
            pendingStyle.setBorderRight(BorderStyle.THIN);

            // 5. REJECTED: Bị từ chối (Màu xám/đỏ nhạt)
            CellStyle rejectedStyle = workbook.createCellStyle();
            Font rejectedFont = workbook.createFont();
            rejectedFont.setBold(true);
            rejectedFont.setColor(IndexedColors.RED.getIndex());
            rejectedStyle.setFont(rejectedFont);
            rejectedStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            rejectedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            rejectedStyle.setAlignment(HorizontalAlignment.CENTER);
            rejectedStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            rejectedStyle.setBorderBottom(BorderStyle.THIN);
            rejectedStyle.setBorderTop(BorderStyle.THIN);
            rejectedStyle.setBorderLeft(BorderStyle.THIN);
            rejectedStyle.setBorderRight(BorderStyle.THIN);

            // Title rows
            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Attendance Report");
            titleCell.setCellStyle(titleStyle);

            Row classRow = sheet.createRow(rowIdx++);
            Cell classCell = classRow.createCell(0);
            classCell.setCellValue("Class: " + sanitize(classEntity.getName()) + " (" + sanitize(classEntity.getClassCode()) + ")");
            classCell.setCellStyle(infoStyle);

            Row sessionRow = sheet.createRow(rowIdx++);
            Cell sessionCell = sessionRow.createCell(0);
            sessionCell.setCellValue("Session: " + sanitize(session.getName()) + " - " + session.getAttendanceDate());
            sessionCell.setCellStyle(infoStyle);

            rowIdx++; // blank row

            // Header row
            Row headerRow = sheet.createRow(rowIdx++);
            String[] headers = {"No.", "Student Code", "Full Name", "Check-in Time", "Check-out Time", "Status", "Notes"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int no = 1;
            int pendingCount = 0;
            int rejectedCount = 0;

            for (Student student : students) {
                Row row = sheet.createRow(rowIdx++);
                AttendanceRecord record = recordMap.get(student.getId());
                List<AttendanceReviewAttempt> studentAttempts = attemptMap.getOrDefault(student.getId(), List.of());

                row.createCell(0).setCellValue(no++);
                row.createCell(1).setCellValue(sanitize(student.getStudentCode()));
                row.createCell(2).setCellValue(sanitize(student.getFullName()));

                if (record != null && record.getCheckInAt() != null) {
                    row.createCell(3).setCellValue(DATETIME_FORMAT.format(record.getCheckInAt()));
                } else {
                    row.createCell(3).setCellValue("");
                }

                if (record != null && record.getCheckOutAt() != null) {
                    row.createCell(4).setCellValue(DATETIME_FORMAT.format(record.getCheckOutAt()));
                } else {
                    row.createCell(4).setCellValue("");
                }

                String status = computeStatusWithReview(record, studentAttempts);
                Cell statusCell = row.createCell(5);
                statusCell.setCellValue(status);

                StringBuilder notes = new StringBuilder();
                if ("PENDING_REVIEW".equals(status)) {
                    pendingCount++;
                    statusCell.setCellStyle(pendingStyle);
                    studentAttempts.stream()
                            .filter(a -> "PENDING_REVIEW".equals(a.getReviewStatus()))
                            .findFirst()
                            .ifPresent(a -> notes.append("Chờ duyệt (").append(a.getStage()).append("): ").append(a.getGpsReason() != null ? a.getGpsReason() : ""));
                } else if ("REJECTED".equals(status)) {
                    rejectedCount++;
                    statusCell.setCellStyle(rejectedStyle);
                    studentAttempts.stream()
                            .filter(a -> "REJECTED".equals(a.getReviewStatus()))
                            .findFirst()
                            .ifPresent(a -> notes.append("Từ chối: ").append(a.getAdminNote() != null ? a.getAdminNote() : ""));
                } else if ("ABSENT".equals(status)) {
                    statusCell.setCellStyle(absentStyle);
                } else if ("MISSING_CHECK_OUT".equals(status) || "MISSING_CHECK_IN".equals(status)) {
                    statusCell.setCellStyle(warningStyle);
                } else if ("PRESENT".equals(status)) {
                    statusCell.setCellStyle(presentStyle);
                }

                row.createCell(6).setCellValue(notes.toString());
            }

            // Summary row
            rowIdx++;
            Row summaryRow = sheet.createRow(rowIdx);
            Cell summaryCell = summaryRow.createCell(0);
            long totalStudents = students.size();
            long checkedIn = records.stream().filter(r -> r.getCheckInAt() != null).count();
            long checkedOut = records.stream().filter(r -> r.getCheckOutAt() != null).count();
            summaryCell.setCellValue(String.format("Total: %d | Checked-in: %d | Checked-out: %d | Pending Review: %d | Rejected: %d",
                    totalStudents, checkedIn, checkedOut, pendingCount, rejectedCount));
            summaryCell.setCellStyle(infoStyle);

            // Auto-size columns
            int[] widths = {2000, 4500, 7500, 6000, 6000, 6500, 8000};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i]);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate report", e);
        }
    }

    private String computeStatusWithReview(AttendanceRecord record, List<AttendanceReviewAttempt> attempts) {
        boolean hasCheckIn = record != null && record.getCheckInAt() != null;
        boolean hasCheckOut = record != null && record.getCheckOutAt() != null;

        if (hasCheckIn && hasCheckOut) return "PRESENT";
        if (hasCheckIn) {
            boolean hasPendingCheckOut = attempts.stream()
                    .anyMatch(a -> a.getStage() == AttendanceStage.CHECK_OUT && "PENDING_REVIEW".equals(a.getReviewStatus()));
            if (hasPendingCheckOut) return "PENDING_REVIEW";
            return "MISSING_CHECK_OUT";
        }
        if (hasCheckOut) return "MISSING_CHECK_IN";

        // Not checked in officially
        boolean hasPending = attempts.stream().anyMatch(a -> "PENDING_REVIEW".equals(a.getReviewStatus()));
        if (hasPending) return "PENDING_REVIEW";

        boolean hasRejected = attempts.stream().anyMatch(a -> "REJECTED".equals(a.getReviewStatus()));
        if (hasRejected) return "REJECTED";

        return "ABSENT";
    }

    /**
     * Protect against spreadsheet formula injection by prefixing dangerous characters.
     */
    private String sanitize(String value) {
        if (value == null) return "";
        if (value.startsWith("=") || value.startsWith("+") ||
            value.startsWith("-") || value.startsWith("@")) {
            return "'" + value;
        }
        return value;
    }
}
