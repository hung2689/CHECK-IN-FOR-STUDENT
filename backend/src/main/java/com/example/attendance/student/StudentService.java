package com.example.attendance.student;

import com.example.attendance.exception.BadRequestException;
import com.example.attendance.exception.ConflictException;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.classmanagement.ClassEntity;
import com.example.attendance.classmanagement.ClassRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassRepository classRepository;

    // Column name mappings (Vietnamese + English)
    private static final Set<String> CODE_COLUMNS = Set.of(
        "studentcode", "mssv", "mã sinh viên", "ma sinh vien", "student_code"
    );
    private static final Set<String> NAME_COLUMNS = Set.of(
        "fullname", "full_name", "họ và tên", "ho va ten", "hovaten", "họ tên", "ho ten", "hoten"
    );
    private static final Set<String> EMAIL_COLUMNS = Set.of(
        "email", "e-mail"
    );

    public StudentService(StudentRepository studentRepository,
                          ClassStudentRepository classStudentRepository,
                          ClassRepository classRepository) {
        this.studentRepository = studentRepository;
        this.classStudentRepository = classStudentRepository;
        this.classRepository = classRepository;
    }

    public List<StudentResponse> getStudentsByClass(Long classId, String query) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        List<Student> students;
        if (query != null && !query.isBlank()) {
            students = studentRepository.searchStudentsByClassId(classId, query.trim());
        } else {
            students = studentRepository.findStudentsByClassId(classId);
        }
        return students.stream().map(StudentResponse::from).toList();
    }

    @Transactional
    public StudentResponse addStudent(Long classId, StudentRequest request) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        String code = request.studentCode().trim();
        String name = request.fullName().trim();

        // Find or create student
        Student student = studentRepository.findByStudentCode(code).orElse(null);
        if (student == null) {
            student = new Student();
            student.setStudentCode(code);
            student.setFullName(name);
            student.setEmail(request.email() != null ? request.email().trim() : null);
            student = studentRepository.save(student);
        }

        // Check enrollment
        if (classStudentRepository.existsByClassIdAndStudentId(classId, student.getId())) {
            throw new ConflictException("Student " + code + " is already enrolled in this class");
        }

        ClassStudent cs = new ClassStudent();
        cs.setClassId(classId);
        cs.setStudentId(student.getId());
        classStudentRepository.save(cs);

        return StudentResponse.from(student);
    }

    @Transactional
    public ImportResult importStudents(Long classId, MultipartFile file) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                throw new BadRequestException("Excel file must have a header row and at least one data row");
            }

            // Parse header
            Row headerRow = sheet.getRow(0);
            int codeCol = -1, nameCol = -1, emailCol = -1;

            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell == null) continue;
                String header = getCellStringValue(cell).trim().toLowerCase();
                if (CODE_COLUMNS.contains(header)) codeCol = i;
                else if (NAME_COLUMNS.contains(header)) nameCol = i;
                else if (EMAIL_COLUMNS.contains(header)) emailCol = i;
            }

            if (codeCol == -1) throw new BadRequestException("Missing required column: studentCode (or MSSV, Mã sinh viên)");
            if (nameCol == -1) throw new BadRequestException("Missing required column: fullName (or Họ và tên)");

            List<ImportResult.ImportError> errors = new ArrayList<>();
            Set<String> seenCodes = new HashSet<>();
            int successCount = 0;
            int skippedCount = 0;
            int totalRows = 0;

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                totalRows++;
                int displayRow = rowIdx + 1;

                String code = getCellStringValue(row.getCell(codeCol)).trim();
                String name = getCellStringValue(row.getCell(nameCol)).trim();
                String email = emailCol >= 0 ? getCellStringValue(row.getCell(emailCol)).trim() : null;

                // Validate
                if (code.isEmpty()) {
                    errors.add(new ImportResult.ImportError(displayRow, "", "Student code is empty"));
                    continue;
                }
                if (name.isEmpty()) {
                    errors.add(new ImportResult.ImportError(displayRow, code, "Full name is empty"));
                    continue;
                }

                // Check duplicate within file
                if (seenCodes.contains(code.toUpperCase())) {
                    errors.add(new ImportResult.ImportError(displayRow, code, "Duplicate student code in file"));
                    skippedCount++;
                    continue;
                }
                seenCodes.add(code.toUpperCase());

                // Find or create student
                Student student = studentRepository.findByStudentCode(code).orElse(null);
                if (student == null) {
                    student = new Student();
                    student.setStudentCode(code);
                    student.setFullName(name);
                    student.setEmail(email != null && !email.isEmpty() ? email : null);
                    student = studentRepository.save(student);
                }

                // Check enrollment
                if (classStudentRepository.existsByClassIdAndStudentId(classId, student.getId())) {
                    errors.add(new ImportResult.ImportError(displayRow, code, "Already enrolled in this class"));
                    skippedCount++;
                    continue;
                }

                ClassStudent cs = new ClassStudent();
                cs.setClassId(classId);
                cs.setStudentId(student.getId());
                classStudentRepository.save(cs);
                successCount++;
            }

            return new ImportResult(totalRows, successCount, skippedCount,
                    errors.size() - skippedCount, errors);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to parse Excel file: " + e.getMessage());
        }
    }

    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Students");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            String[] headers = {"studentCode", "fullName", "email"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Example row
            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("SE001");
            example.createCell(1).setCellValue("Nguyen Van A");
            example.createCell(2).setCellValue("a@example.com");

            // Auto-size
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 6000);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate template", e);
        }
    }

    @Transactional
    public StudentResponse updateStudent(Long classId, Long studentId, StudentRequest request) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        String newCode = request.studentCode().trim().toUpperCase();
        if (!student.getStudentCode().equalsIgnoreCase(newCode)) {
            if (studentRepository.existsByStudentCode(newCode)) {
                throw new ConflictException("Student code already exists: " + newCode);
            }
            student.setStudentCode(newCode);
        }

        student.setFullName(request.fullName().trim());
        student.setEmail(request.email() != null && !request.email().isBlank() ? request.email().trim() : null);

        student = studentRepository.save(student);
        return StudentResponse.from(student);
    }

    @Transactional
    public void removeStudentFromClass(Long classId, Long studentId) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if (!classStudentRepository.existsByClassIdAndStudentId(classId, studentId)) {
            throw new ResourceNotFoundException("Student is not enrolled in this class");
        }

        classStudentRepository.deleteByClassIdAndStudentId(classId, studentId);
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}

