package dev.bauhd.velocityupdater;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public final class PathUtil {

  public static void copyFileTree(final Path source, final Path target) throws IOException {
    Files.walkFileTree(source,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(final Path path,
              final BasicFileAttributes attributes) throws IOException {
            Files.createDirectories(target.resolve(source.relativize(path)));
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(final Path path,
              final BasicFileAttributes attributes) throws IOException {
            Files.copy(path, target.resolve(source.relativize(path)));
            return FileVisitResult.CONTINUE;
          }
        });
  }

  public static void deleteDirectory(final Path directory) throws IOException {
    Files.walkFileTree(directory, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult postVisitDirectory(final Path path, final IOException exception)
          throws IOException {
        Files.delete(path);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(final Path path, final BasicFileAttributes attributes)
          throws IOException {
        Files.delete(path);
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
