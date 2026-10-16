package com.example.attendance.classmanagement;

import com.example.attendance.exception.ConflictException;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.student.ClassStudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClassService {

    private final ClassRepository classRepository;
    private final ClassStudentRepository classStudentRepository;

    public ClassService(ClassRepository classRepository, ClassStudentRepository classStudentRepository) {
        this.classRepository = classRepository;
        this.classStudentRepository = classStudentRepository;
    }

    public List<ClassResponse> getAllClasses() {
        return classRepository.findByActiveTrue().stream()
                .map(c -> ClassResponse.from(c, classStudentRepository.countByClassId(c.getId())))
                .toList();
    }

    public ClassResponse getClassById(Long id) {
        ClassEntity entity = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));
        return ClassResponse.from(entity, classStudentRepository.countByClassId(id));
    }

    @Transactional
    public ClassResponse createClass(ClassRequest request, Long createdBy) {
        if (classRepository.existsByClassCode(request.classCode())) {
            throw new ConflictException("Class code already exists: " + request.classCode());
        }
        ClassEntity entity = new ClassEntity();
        entity.setClassCode(request.classCode().trim());
        entity.setName(request.name().trim());
        entity.setDescription(request.description());
        entity.setCreatedBy(createdBy);
        entity = classRepository.save(entity);
        return ClassResponse.from(entity, 0);
    }

    @Transactional
    public ClassResponse updateClass(Long id, ClassRequest request) {
        ClassEntity entity = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));

        // Check if class code changed and new code already exists
        if (!entity.getClassCode().equals(request.classCode()) &&
                classRepository.existsByClassCode(request.classCode())) {
            throw new ConflictException("Class code already exists: " + request.classCode());
        }

        entity.setClassCode(request.classCode().trim());
        entity.setName(request.name().trim());
        entity.setDescription(request.description());
        entity = classRepository.save(entity);
        return ClassResponse.from(entity, classStudentRepository.countByClassId(id));
    }

    public ClassEntity getEntity(Long id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));
    }
}
