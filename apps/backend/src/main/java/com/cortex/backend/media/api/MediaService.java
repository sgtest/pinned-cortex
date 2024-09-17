package com.cortex.backend.media.api;

import com.cortex.backend.media.domain.Media;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface MediaService {


  List<Media> getAllMedia();

  Optional<Media> getMediaById(Long id);


  Media uploadMedia(MultipartFile file, String altText, String folder, String subfolder)
      throws IOException;

  Media updateMedia(Long id, String newAltText);


  void deleteMedia(Long id);

  String createResizedImageUrl(String publicId, int width, int height);
}