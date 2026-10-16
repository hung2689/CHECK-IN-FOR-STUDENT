package com.example.attendance.classmanagement;

import com.example.attendance.auth.User;
import com.example.attendance.auth.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;
    private final UserRepository userRepository;

    public ClassController(ClassService classService, UserRepository userRepository) {
        this.classService = classService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<ClassResponse>> getAll() {
        return ResponseEntity.ok(classService.getAllClasses());
    }

    @GetMapping("/{classId}")
    public ResponseEntity<ClassResponse> getById(@PathVariable Long classId) {
        return ResponseEntity.ok(classService.getClassById(classId));
    }

    @PostMapping
    public ResponseEntity<ClassResponse> create(@Valid @RequestBody ClassRequest request,
                                                  Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(classService.createClass(request, user.getId()));
    }

    @PutMapping("/{classId}")
    public ResponseEntity<ClassResponse> update(@PathVariable Long classId,
                                                 @Valid @RequestBody ClassRequest request) {
        return ResponseEntity.ok(classService.updateClass(classId, request));
    }
}
