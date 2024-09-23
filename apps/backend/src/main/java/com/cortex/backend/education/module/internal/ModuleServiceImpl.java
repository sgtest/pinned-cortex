package com.cortex.backend.education.module.internal;

import com.cortex.backend.common.SlugUtils;
import com.cortex.backend.education.course.domain.Course;
import com.cortex.backend.education.course.internal.CourseRepository;
import com.cortex.backend.education.module.api.ModuleService;
import com.cortex.backend.education.module.api.dto.ModuleRequest;
import com.cortex.backend.education.module.api.dto.ModuleResponse;
import com.cortex.backend.education.module.domain.ModuleEntity;
import com.cortex.backend.media.api.MediaService;
import com.cortex.backend.media.domain.Media;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModuleServiceImpl implements ModuleService {

  private final ModuleRepository moduleRepository;
  private final ModuleMapper moduleMapper;
  private final CourseRepository courseRepository;
  private final MediaService mediaService;
  private final SlugUtils slugUtils;

  @Override
  @Transactional(readOnly = true)
  public List<ModuleResponse> getAllModules() {
    return StreamSupport.stream(moduleRepository.findAll().spliterator(), false)
        .map(moduleMapper::toModuleResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ModuleResponse> getModuleById(Long id) {
    return moduleRepository.findById(id)
        .map(moduleMapper::toModuleResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ModuleResponse> getModuleBySlug(String slug) {
    return moduleRepository.findBySlug(slug)
        .map(moduleMapper::toModuleResponse);
  }

  @Override
  @Transactional
  public ModuleResponse createModule(ModuleRequest request, MultipartFile image) throws IOException {
    ModuleEntity module = new ModuleEntity();
    module.setName(request.getName());
    module.setDescription(request.getDescription());
    module.setSlug(generateUniqueSlug(request.getName()));
    setCourse(module, request.getCourseId());

    handleImageUpload(module, image, request.getImageAltText());

    ModuleEntity savedModule = moduleRepository.save(module);
    return moduleMapper.toModuleResponse(savedModule);
  }

  @Override
  @Transactional
  public ModuleResponse updateModule(Long id, ModuleRequest request, MultipartFile image) throws IOException {
    ModuleEntity existingModule = moduleRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Module not found"));

    if (request.getName() != null) {
      existingModule.setName(request.getName());
      if (!existingModule.getName().equals(request.getName())) {
        existingModule.setSlug(generateUniqueSlug(request.getName(), existingModule.getSlug()));
      }
    }
    if (request.getDescription() != null) {
      existingModule.setDescription(request.getDescription());
    }
    if (request.getCourseId() != null) {
      setCourse(existingModule, request.getCourseId());
    }

    handleImageUpload(existingModule, image, request.getImageAltText());

    ModuleEntity updatedModule = moduleRepository.save(existingModule);
    return moduleMapper.toModuleResponse(updatedModule);
  }

  @Override
  @Transactional
  public void deleteModule(Long id) {
    ModuleEntity module = moduleRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Module not found"));
    moduleRepository.delete(module);
  }

  private String generateUniqueSlug(String name) {
    return slugUtils.generateUniqueSlug(name, slug -> moduleRepository.findBySlug(slug).isPresent());
  }

  private String generateUniqueSlug(String name, String currentSlug) {
    return slugUtils.generateUniqueSlug(name,
        slug -> !slug.equals(currentSlug) && moduleRepository.findBySlug(slug).isPresent());
  }
  
  private void setCourse(ModuleEntity module, Long courseId) {
    Course course = courseRepository.findById(courseId)
        .orElseThrow(() -> new EntityNotFoundException("Course not found"));
    module.setCourse(course);
  }

  private void handleImageUpload(ModuleEntity module, MultipartFile image, String altText) throws IOException {
    if (image != null && !image.isEmpty()) {
      Media uploadedMedia = mediaService.uploadMedia(image, altText, "modules", module.getSlug());
      module.setImage(uploadedMedia);
    }
  }
}