package com.cortex.backend.progress.domain;

import com.cortex.backend.education.roadmap.domain.Roadmap;
import com.cortex.backend.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "user_roadmap_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoadmapProgress {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne
  @JoinColumn(name = "roadmap_id", nullable = false)
  private Roadmap roadmap;

  @Column(name = "courses_completed", nullable = false)
  private Integer coursesCompleted;

  @Column(name = "last_updated")
  private LocalDate lastUpdated;
}