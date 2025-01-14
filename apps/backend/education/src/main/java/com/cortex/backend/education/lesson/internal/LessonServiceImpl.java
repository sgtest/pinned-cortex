package com.cortex.backend.education.lesson.internal;

import com.cortex.backend.core.common.PageResponse;
import com.cortex.backend.core.common.SlugUtils;
import com.cortex.backend.core.domain.EntityType;
import com.cortex.backend.education.lesson.api.LessonRepository;
import com.cortex.backend.education.lesson.api.LessonService;
import com.cortex.backend.education.lesson.api.dto.LessonRequest;
import com.cortex.backend.education.lesson.api.dto.LessonResponse;
import com.cortex.backend.education.lesson.api.dto.LessonUpdateRequest;
import com.cortex.backend.core.domain.Lesson;
import com.cortex.backend.core.domain.ModuleEntity;
import com.cortex.backend.education.module.api.ModuleRepository;
import com.cortex.backend.education.progress.api.LessonCompletedEvent;
import com.cortex.backend.education.progress.api.ProgressTrackingService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

  private final LessonRepository lessonRepository;
  private final LessonMapper lessonMapper;
  private final ModuleRepository moduleRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final SlugUtils slugUtils;

  private static final String LESSON_NOT_FOUND_MESSAGE = "Lesson not found";

  @Override
  @Transactional(readOnly = true)
  public PageResponse<LessonResponse> getAllLessons(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

    Page<Lesson> lessons = lessonRepository.findAllPublishedLessons(pageable);

    List<LessonResponse> response = lessons.stream()
        .map(lessonMapper::toLessonResponse)
        .toList();

    return new PageResponse<>(response, lessons.getNumber(), lessons.getSize(),
        lessons.getTotalElements(), lessons.getTotalPages(), lessons.isFirst(), lessons.isLast());


  }

  @Override
  @Transactional(readOnly = true)
  public Optional<LessonResponse> getLessonById(Long id) {
    return lessonRepository.findById(id)
        .map(lessonMapper::toLessonResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<LessonResponse> getLessonBySlug(String slug) {
    return lessonRepository.findBySlug(slug)
        .map(lessonMapper::toLessonResponse);
  }

  @Override
  @Transactional
  public LessonResponse createLesson(LessonRequest request) {
    Lesson lesson = new Lesson();
    lesson.setName(request.getName());
    lesson.setContent(request.getContent());
    lesson.setCredits(request.getCredits());
    lesson.setIsPublished(request.isPublished());
    lesson.setSlug(generateUniqueSlug(request.getName()));
    setLessonModule(lesson, request.getModuleId());
    Lesson savedLesson = lessonRepository.save(lesson);
    return lessonMapper.toLessonResponse(savedLesson);
  }

  @Override
  @Transactional
  public LessonResponse updateLesson(Long id, LessonUpdateRequest request) {
    Lesson existingLesson = lessonRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException(LESSON_NOT_FOUND_MESSAGE));

    if (request.getName() != null) {
      existingLesson.setName(request.getName());
      existingLesson.setSlug(generateUniqueSlug(request.getName(), existingLesson.getSlug()));
    }
    if (request.getContent() != null) {
      existingLesson.setContent(request.getContent());
    }
    if (request.getCredits() != null) {
      existingLesson.setCredits(request.getCredits());
    }
    if (request.getModuleId() != null) {
      setLessonModule(existingLesson, request.getModuleId());
    }

    if (request.getIsPublished() != null) {
      existingLesson.setIsPublished(request.getIsPublished());
    }

    Lesson updatedLesson = lessonRepository.save(existingLesson);
    return lessonMapper.toLessonResponse(updatedLesson);
  }

  @Override
  @Transactional
  public void deleteLesson(Long id) {
    Lesson lesson = lessonRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException(LESSON_NOT_FOUND_MESSAGE));
    lessonRepository.delete(lesson);
  }

  @Override
  @Transactional
  public void completeLesson(Long lessonId, Long userId) {
    lessonRepository.findById(lessonId)
        .orElseThrow(() -> new EntityNotFoundException(LESSON_NOT_FOUND_MESSAGE));
    eventPublisher.publishEvent(new LessonCompletedEvent(lessonId, userId));
  }

  @Override
  public Long getModuleIdForLesson(Long lessonId) {
    return lessonRepository.findById(lessonId)
        .map(lesson -> lesson.getModuleEntity().getId())
        .orElseThrow(() -> new EntityNotFoundException("Lesson not found"));
  }

  private String generateUniqueSlug(String name) {
    return slugUtils.generateUniqueSlug(name,
        slug -> lessonRepository.findBySlug(slug).isPresent());
  }

  private String generateUniqueSlug(String name, String currentSlug) {
    return slugUtils.generateUniqueSlug(name,
        slug -> !slug.equals(currentSlug) && lessonRepository.findBySlug(slug).isPresent());
  }

  private void setLessonModule(Lesson lesson, Long moduleId) {
    ModuleEntity module = moduleRepository.findById(moduleId)
        .orElseThrow(() -> new EntityNotFoundException("Module not found"));
    lesson.setModuleEntity(module);
  }
}