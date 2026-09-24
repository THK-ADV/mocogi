package ops

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Comparator

import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import scala.util.Try
import scala.util.Using

object FileOps {

  /** Resolves a simple PDF filename inside a configured folder, including symlink checks. */
  def resolvePdfFile(filename: String, rootFolderPath: String): Option[Path] =
    if filename.isEmpty || filename != filename.trim || !filename.endsWith(".pdf") ||
      filename.exists(c => c == '/' || c == '\\' || c == '\u0000')
    then None
    else
      Try {
        val base     = Paths.get(rootFolderPath).toAbsolutePath.normalize().toRealPath()
        val resolved = base.resolve(filename).normalize().toRealPath()
        Option.when(resolved.startsWith(base) && Files.isRegularFile(resolved))(resolved)
      }.toOption.flatten

  /**
   * Creates a new temporary tex file with the specified filename in its own folder.
   */
  def createLatexFile(filename: String, rootFolderPath: String): Path = {
    val root = Files.createDirectories(Paths.get(rootFolderPath))
    val dir  = Files.createTempDirectory(root, "latex-")
    try Files.createFile(dir.resolve(s"$filename.tex"))
    catch {
      case NonFatal(error) =>
        Files.deleteIfExists(dir)
        throw error
    }
  }

  /**
   * Creates a new temporary file
   */
  def createRandomFile(rootFolderPath: String): Path = {
    val path = Paths.get(rootFolderPath).resolve(System.currentTimeMillis().toString)
    Files.createFile(path)
  }

  extension (self: Path) {
    def rename(newName: String) =
      Files.move(
        self,
        self.resolveSibling(newName),
        StandardCopyOption.REPLACE_EXISTING
      )

    def move(folder: Path) =
      try {
        Right(
          Files.move(
            self,
            folder.resolve(self.getFileName),
            StandardCopyOption.REPLACE_EXISTING
          )
        )
      } catch {
        case NonFatal(e) => Left(e.getMessage)
      }

    def copy(folder: Path) =
      try {
        Right(
          Files.copy(
            self,
            folder.resolve(self.getFileName),
            StandardCopyOption.REPLACE_EXISTING
          )
        )
      } catch {
        case NonFatal(e) => Left(e.getMessage)
      }

    def deleteDirectory(): Unit =
      Using.resource(Files.walk(self)) { paths =>
        paths.sorted(Comparator.reverseOrder()).forEach(p => Files.deleteIfExists(p))
      }

    def deleteContentsOfDirectory(): Unit =
      if (Files.isDirectory(self))
        Files
          .walk(self)
          .filter(p => self.toAbsolutePath != p.toAbsolutePath)
          .forEach(p => p.deleteContentsOfDirectory())
      else Files.deleteIfExists(self)

    def foreachFileOfDirectory(f: Path => Unit): Unit = {
      Files.walk(self).toList.asScala.toVector.collect {
        case file if !Files.isDirectory(file) && !Files.isHidden(file) => f(file)
      }
      ()
    }

    def getFilesOfDirectory(): Vector[Path] =
      Files.walk(self).toList.asScala.toVector.collect {
        case file if !Files.isDirectory(file) && !Files.isHidden(file) => file
      }

    def getFilesOfDirectory[A](p: Path => Boolean)(f: Path => A): Vector[A] =
      Files
        .walk(self)
        .toList
        .asScala
        .toVector
        .collect {
          case file if !Files.isDirectory(file) && !Files.isHidden(file) && p(file) => f(file)
        }

    def createFile(
        name: String,
        content: String
    ): Either[String, Path] = {
      try {
        val file = self.resolve(name)
        Files.deleteIfExists(file)
        val path = Files.createFile(file)
        Files.writeString(path, content)
        Right(path)
      } catch {
        case NonFatal(e) =>
          Left(e.getMessage)
      }
    }
  }
}
