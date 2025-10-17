package com.example.jxlsstream.excel;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.poi.util.TempFile;
import org.apache.poi.util.TempFileCreationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PoiTempFileConfigurer {
  private static final Logger log = LoggerFactory.getLogger(PoiTempFileConfigurer.class);
  private static final Path PROJECT_ROOT = Paths.get("").toAbsolutePath();
  private static final Path BASE_TEMP_DIR = PROJECT_ROOT.resolve("tmp").resolve("poi");

  @PostConstruct
  void configurePoiTempDirectory() {
    try {
      Files.createDirectories(BASE_TEMP_DIR);
      String previousTmpDir = System.getProperty("java.io.tmpdir");
      System.setProperty("java.io.tmpdir", BASE_TEMP_DIR.toString());
      TempFile.setTempFileCreationStrategy(new ProjectRelativeTempFileCreationStrategy(BASE_TEMP_DIR));
      log.info(
          "Apache POI temporary files will be created under {} (java.io.tmpdir was: {})",
          BASE_TEMP_DIR,
          previousTmpDir);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Failed to configure Apache POI temporary directory under " + BASE_TEMP_DIR, e);
    }
  }

  private static final class ProjectRelativeTempFileCreationStrategy
      implements TempFileCreationStrategy {
    private final Path baseDir;

    private ProjectRelativeTempFileCreationStrategy(Path baseDir) {
      this.baseDir = baseDir;
    }

    @Override
    public File createTempFile(String prefix, String suffix) throws IOException {
      return createTempFile(prefix, suffix, baseDir.toFile());
    }

    @Override
    public File createTempFile(String prefix, String suffix, File directory) throws IOException {
      Path targetDir = directory == null ? baseDir : directory.toPath();
      Files.createDirectories(targetDir);
      Path tempFile = Files.createTempFile(targetDir, normalized(prefix), suffix);
      File file = tempFile.toFile();
      file.deleteOnExit();
      return file;
    }

    @Override
    public File createTempDirectory(String prefix) throws IOException {
      Path dir = Files.createTempDirectory(baseDir, normalized(prefix));
      File directory = dir.toFile();
      directory.deleteOnExit();
      return directory;
    }

    private String normalized(String prefix) {
      String candidate = prefix == null ? "tmp" : prefix;
      if (candidate.length() < 3) {
        candidate = (candidate + "___").substring(0, 3);
      }
      return candidate;
    }
  }
}
