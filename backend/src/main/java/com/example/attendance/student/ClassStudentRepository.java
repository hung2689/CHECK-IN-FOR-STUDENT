package com.example.attendance.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ClassStudentRepository extends JpaRepository<ClassStudent, Long> {
    boolean existsByClassIdAndStudentId(Long classId, Long studentId);
    List<ClassStudent> findByClassId(Long classId);
    Optional<ClassStudent> findByClassIdAndStudentId(Long classId, Long studentId);

    @Query("SELECT cs.studentId FROM ClassStudent cs WHERE cs.classId = :classId")
    List<Long> findStudentIdsByClassId(@Param("classId") Long classId);

    long countByClassId(Long classId);

    @Modifying
    @Query("DELETE FROM ClassStudent cs WHERE cs.classId = :classId AND cs.studentId = :studentId")
    void deleteByClassIdAndStudentId(@Param("classId") Long classId, @Param("studentId") Long studentId);
}
