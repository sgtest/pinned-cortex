package com.cortex.backend.engine.domain;

import com.cortex.backend.education.solution.Solution;
import com.cortex.backend.entities.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "submission")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Submission extends BaseEntity {

  @Column(nullable = false, columnDefinition = "TEXT")
  private String code;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "language_id", nullable = false)
  private Language language;

  @Column(columnDefinition = "TEXT")
  private String stdin;

  @Column(name = "expected_output", columnDefinition = "TEXT")
  private String expectedOutput;

  @Column(name = "cpu_time_limit")
  private Float cpuTimeLimit;

  @Column(name = "cpu_extra_time")
  private Float cpuExtraTime;

  @Column(name = "command_line_arguments")
  private String commandLineArguments;

  @Column(name = "compiler_options")
  private String compilerOptions;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "solution_id", nullable = false)
  private Solution solution;
}