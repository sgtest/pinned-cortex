package com.cortex.backend.education.lesson.api;

import com.cortex.backend.education.lesson.api.dto.LessonRequest;
import com.cortex.backend.education.lesson.api.dto.LessonResponse;
import com.cortex.backend.education.lesson.api.dto.LessonUpdateRequest;
import java.util.List;
import java.util.Optional;

public interface LessonService {

  List<LessonResponse> getAllLessons();

  Optional<LessonResponse> getLessonById(Long id);

  Optional<LessonResponse> getLessonBySlug(String slug);

  LessonResponse createLesson(LessonRequest lesson);

  LessonResponse updateLesson(Long id, LessonUpdateRequest lessonRequest);

  void deleteLesson(Long id);
}