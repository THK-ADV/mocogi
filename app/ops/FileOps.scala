package ops

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Comparator

import scala.annotation.unused
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

object FileOps {
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
          .forEach(p => Files.deleteIfExists(p))
      else ()

    def foreachFileOfDirectory(f: Path => Unit) = {
      Files.walk(self).toList.asScala.toVector.collect {
        case file if !Files.isDirectory(file) => f(file)
      }
    }

    @unused
    def getFilesOfDirectory(): Vector[Path] =
      Files.walk(self).toList.asScala.toVector.collect {
        case file if !Files.isDirectory(file) => file
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
