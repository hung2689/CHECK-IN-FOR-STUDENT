package com.example.attendance.report;

import com.example.attendance.attendance.*;
import com.example.attendance.classmanagement.ClassEntity;
import com.example.attendance.classmanagement.ClassRepository;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.student.ClassStudentRepository;
import com.example.attendance.student.Student;
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
    private final ClassRepository classRepository;
    private final com.example.attendance.student.StudentRepository studentRepository;

    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    public ReportService(AttendanceSessionRepository sessionRepository,
                         AttendanceRecordRepository recordRepository,
                         ClassRepository classRepository,
                         com.example.attendance.student.StudentRepository studentRepository) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
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

        Map<Long, AttendanceRecord> recordMap = new HashMap<>();
        for (AttendanceRecord r : records) {
            recordMap.put(r.getStudentId(), r);
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
            for (Student student : students) {
                Row row = sheet.createRow(rowIdx++);
                AttendanceRecord record = recordMap.get(student.getId());

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

                String status = computeStatus(record, session);
                row.createCell(5).setCellValue(status);
                row.createCell(6).setCellValue(""); // Notes
            }

            // Summary row
            rowIdx++;
            Row summaryRow = sheet.createRow(rowIdx);
            Cell summaryCell = summaryRow.createCell(0);
            long totalStudents = students.size();
            long checkedIn = records.stream().filter(r -> r.getCheckInAt() != null).count();
            long checkedOut = records.stream().filter(r -> r.getCheckOutAt() != null).count();
            summaryCell.setCellValue("Total: " + totalStudents + " | Checked-in: " + checkedIn + " | Checked-out: " + checkedOut);
            summaryCell.setCellStyle(infoStyle);

            // Auto-size columns
            int[] widths = {2000, 5000, 8000, 6000, 6000, 5000, 5000};
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

    private String computeStatus(AttendanceRecord record, AttendanceSession session) {
        if (record == null) return "ABSENT";
        boolean hasCheckIn = record.getCheckInAt() != null;
        boolean hasCheckOut = record.getCheckOutAt() != null;

        if (hasCheckIn && hasCheckOut) return "PRESENT";
        if (hasCheckIn) return "MISSING_CHECK_OUT";
        if (hasCheckOut) return "MISSING_CHECK_IN";
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
