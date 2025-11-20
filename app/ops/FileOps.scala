package ops

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Comparator

import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

object FileOps {

  /**
   * Creates a new temporary tex file with the specified filename in a new a folder
   */
  def createLatexFile(filename: String, rootFolderPath: String): Path = {
    val newDir = Files.createDirectories(Paths.get(rootFolderPath).resolve(System.currentTimeMillis().toString))
    Files.createFile(newDir.resolve(s"$filename.tex"))
  }

  /**
   * Creates a new temporary file
   */
  def createRandomFile(rootFolderPath: String): Path = {
    val path = Paths.get(rootFolderPath).resolve(System.currentTimeMillis().toString)
    Files.createFile(path)
  }

  implicit class FileOps0(private val self: Path) extends AnyVal {
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
      Files
        .walk(self)
        .sorted(Comparator.reverseOrder())
        .forEach(p => Files.deleteIfExists(p))

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
