package com.example.attendance.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentCode(String studentCode);
    boolean existsByStudentCode(String studentCode);
    List<Student> findByStudentCodeIn(List<String> studentCodes);

    @Query("SELECT s FROM Student s, ClassStudent cs WHERE s.id = cs.studentId AND cs.classId = :classId ORDER BY s.studentCode")
    List<Student> findStudentsByClassId(@Param("classId") Long classId);

    @Query("SELECT s FROM Student s, ClassStudent cs WHERE s.id = cs.studentId AND cs.classId = :classId AND (LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY s.studentCode")
    List<Student> searchStudentsByClassId(@Param("classId") Long classId, @Param("query") String query);
}
