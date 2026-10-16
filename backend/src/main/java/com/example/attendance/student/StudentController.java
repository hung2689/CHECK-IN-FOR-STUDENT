package com.example.attendance.student;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/classes/{classId}/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<List<StudentResponse>> getStudents(
            @PathVariable Long classId,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(studentService.getStudentsByClass(classId, query));
    }

    @PostMapping
    public ResponseEntity<StudentResponse> addStudent(
            @PathVariable Long classId,
            @Valid @RequestBody StudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studentService.addStudent(classId, request));
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResult> importStudents(
            @PathVariable Long classId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(studentService.importStudents(classId, file));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] template = studentService.generateTemplate();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student_template.xlsx")
                .body(template);
    }
    @PutMapping("/{studentId}")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long classId,
            @PathVariable Long studentId,
            @Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(classId, studentId, request));
    }

    @DeleteMapping("/{studentId}")
    public ResponseEntity<Void> removeStudent(
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        studentService.removeStudentFromClass(classId, studentId);
        return ResponseEntity.noContent().build();
    }
}

