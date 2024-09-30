package com.cortex.backend.engine.internal.services;

import com.cortex.backend.core.common.exception.GitSyncException;
import com.cortex.backend.engine.api.ExerciseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class GithubSyncService {

  private final ExerciseService exerciseService;

  @Value("${github.exercises.repo-url}")
  private String repoUrl;

  @Value("${github.exercises.local-path}")
  private String localPathString;

  @Value("${github.exercises.branch}")
  private String branch;


  private String lastSyncedCommit;

  @EventListener(ApplicationReadyEvent.class)
  public void initializeExercises() {
    if (!exerciseService.areLessonsAvailable()) {
      log.warn("No lessons found in the database. Skipping exercise initialization.");
      return;
    }

    if (exerciseService.isExerciseRepositoryEmpty()) {
      log.info("Exercise repository is empty but lessons are available. Initializing exercises...");
      Path localPath = Path.of(localPathString);
      if (!Files.exists(localPath)) {
        log.info("Local repository does not exist. Cloning from GitHub...");
        cloneRepository(localPath);
      }
      forceUpdateExercises(localPath);
    } else {
      log.info("Exercises already exist. Proceeding with normal sync.");
      syncExercises();
    }
  }

  private void forceUpdateExercises(Path localPath) {
    log.info("Forcing update of all exercises from local repository");
    updateExercisesFromLocalRepo(localPath);
  }
  
  
  @Scheduled(fixedRateString = "${github.exercises.sync-interval-ms}")
  public void scheduledSync() {
    if (!exerciseService.areLessonsAvailable()) {
      log.warn("No lessons found in the database. Skipping scheduled sync.");
      return;
    }
    log.info("Starting scheduled sync of exercises");
    syncExercises();
  }

  public void syncExercises() {
    Path localPath = Path.of(localPathString);
    log.info("Checking for updates in repository at {}", localPath);
    try {
      if (pullLatestChanges(localPath)) {
        updateExercisesFromLocalRepo(localPath);
      } else {
        log.info("No new changes in the repository. Skipping update.");
      }
    } catch (Exception e) {
      log.error("Failed to sync exercises", e);
      throw new GitSyncException("Failed to sync exercises", e);
    }
  }

  private boolean pullLatestChanges(Path localPath) throws Exception {
    try (Repository repository = new FileRepositoryBuilder()
        .setGitDir(new File(localPath.toFile(), ".git"))
        .build();
        Git git = new Git(repository)) {

      // Verificar y configurar el remote 'origin'
      StoredConfig config = repository.getConfig();
      String remoteUrl = config.getString("remote", "origin", "url");
      if (remoteUrl == null || remoteUrl.isEmpty()) {
        log.warn("Remote 'origin' not configured. Attempting to add it.");
        git.remoteAdd()
            .setName("origin")
            .setUri(new URIish(repoUrl))
            .call();
        log.info("Added remote 'origin' with URL: {}", repoUrl);
      }

      // Fetch cambios del remoto
      git.fetch().call();

      // Obtener el commit actual
      ObjectId oldHead = repository.resolve("HEAD");

      // Obtener el último commit del remoto
      ObjectId remoteHead = repository.resolve("origin/" + branch);

      if (remoteHead == null) {
        log.warn("Remote branch not found. Attempting to set upstream branch.");
        git.branchCreate()
            .setName(branch)
            .setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
            .setStartPoint("origin/" + branch)
            .setForce(true)
            .call();
        git.pull().setRemoteBranchName(branch).call();
        remoteHead = repository.resolve("origin/" + branch);
      }

      if (!remoteHead.equals(oldHead)) {
        // Hay cambios, realizar pull
        git.pull().setRemoteBranchName(branch).call();

        RevCommit latestCommit = git.log().setMaxCount(1).call().iterator().next();
        String newCommitId = latestCommit.getName();
        log.info("New changes detected. Latest commit: {}", newCommitId);
        lastSyncedCommit = newCommitId;
        return true;
      }

      log.info("No new changes detected in the remote repository.");
      return false;
    } catch (Exception e) {
      log.error("Error pulling latest changes", e);
      throw e;
    }
  }

  private void cloneRepository(Path localPath) {
    try (Git git = Git.cloneRepository()
        .setURI(repoUrl)
        .setDirectory(localPath.toFile())
        .setBranch(branch)
        .call()) {
      log.info("Successfully cloned repository to {}", localPath);
      RevCommit latestCommit = git.log().setMaxCount(1).call().iterator().next();
      lastSyncedCommit = latestCommit.getName();
      log.info("Latest commit after cloning: {}", lastSyncedCommit);
    } catch (Exception e) {
      log.error("Failed to clone repository", e);
      throw new GitSyncException("Failed to clone repository", e);
    }
  }

  private void updateExercisesFromLocalRepo(Path localPath) {
    log.info("Updating exercises from local repository");
    File exercisesDir = localPath.resolve("exercises").toFile();
    log.info("Exercises directory: {}", exercisesDir);

    if (isInvalidDirectory(exercisesDir)) {
      log.warn("Exercises directory does not exist or is not a directory: {}", exercisesDir);
      return;
    }

    int updatedCount = processLanguageDirectories(exercisesDir);
    log.info("Updated or created {} exercises", updatedCount);
  }

  private boolean isInvalidDirectory(File directory) {
    return !directory.exists() || !directory.isDirectory();
  }

  private int processLanguageDirectories(File exercisesDir) {
    int totalUpdated = 0;
    for (File languageDir : Objects.requireNonNull(exercisesDir.listFiles(File::isDirectory))) {
      if (isHiddenOrSystemDirectory(languageDir)) {
        continue;
      }
      totalUpdated += processPracticeDirectory(languageDir);
    }
    return totalUpdated;
  }
  private int processPracticeDirectory(File languageDir) {
    log.info("Processing language directory: {}", languageDir.getName());
    File practiceDir = new File(languageDir, "practice");
    log.info("Practice directory: {}", practiceDir);

    if (isInvalidDirectory(practiceDir)) {
      log.warn("Practice directory does not exist or is not a directory: {}", practiceDir);
      return 0;
    }

    return processExerciseDirectories(practiceDir, languageDir.getName());
  }

  private int processExerciseDirectories(File practiceDir, String languageName) {
    int updatedCount = 0;
    for (File exerciseDir : Objects.requireNonNull(practiceDir.listFiles(File::isDirectory))) {
      if (isHiddenOrSystemDirectory(exerciseDir)) {
        continue;
      }
      log.info("Processing exercise: {}", exerciseDir.getName());
      if (updateExercise(exerciseDir, languageName)) {
        updatedCount++;
      }
    }
    return updatedCount;
  }


  private boolean isHiddenOrSystemDirectory(File directory) {
    return directory.isHidden() || directory.getName().startsWith(".");
  }

  private boolean updateExercise(File exerciseDir, String language) {
    String exerciseName = exerciseDir.getName();
    String githubPath = "exercises" + File.separatorChar + language + File.separatorChar + "practice" + File.separatorChar + exerciseName;
    String instructions = readFileContent(exerciseDir, ".docs/instructions.md");
    String hints = readFileContent(exerciseDir, ".docs/hints.md");

    log.info("Updating exercise: {} ({})", exerciseName, githubPath);
    log.debug("Instructions length: {}, Hints length: {}", instructions.length(), hints.length());

    if (instructions.isEmpty() && hints.isEmpty()) {
      log.warn("Skipping exercise {} as both instructions and hints are empty", exerciseName);
      return false;
    }

    exerciseService.updateOrCreateExercise(exerciseName, githubPath, instructions, hints);
    return true;
  }

  private String readFileContent(File exerciseDir, String relativePath) {
    try {
      Path filePath = exerciseDir.toPath().resolve(relativePath);
      return Files.exists(filePath) ? Files.readString(filePath) : "";
    } catch (Exception e) {
      log.error("Error reading file: {}", relativePath, e);
      return "";
    }
  }
}