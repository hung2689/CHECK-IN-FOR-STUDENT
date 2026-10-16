package com.example.attendance.classmanagement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ClassRepository extends JpaRepository<ClassEntity, Long> {
    Optional<ClassEntity> findByClassCode(String classCode);
    boolean existsByClassCode(String classCode);
    List<ClassEntity> findByActiveTrue();

    @Query("SELECT COUNT(cs) FROM ClassStudent cs WHERE cs.classId = :classId")
    long countStudentsByClassId(Long classId);
}
