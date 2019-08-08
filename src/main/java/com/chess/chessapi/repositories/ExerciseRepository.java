package com.chess.chessapi.repositories;

import com.chess.chessapi.entities.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise,Long> {
    @Query(value ="Select * From exercise where course_id = ?1" ,nativeQuery = true)
    List<Exercise> findByCourseId(long courseId);

    @Query(value ="Select If(count(course_id) > 0, course_id,0) From exercise where id = ?1" ,nativeQuery = true)
    Long findCourseIdByExerciseId(long exerciseId);

    @Query(value ="Select id From exercise where course_id = ?1" ,nativeQuery = true)
    List<Long> findExerciseIdsByCourseId(long courseId);
}