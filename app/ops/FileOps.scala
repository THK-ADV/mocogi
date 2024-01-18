package ops

import java.nio.file.{Files, Path, StandardCopyOption}
import java.util.Comparator
import scala.util.control.NonFatal

object FileOps {
  implicit class FileOps0(private val self: Path) extends AnyVal {
    def rename(newName: String) =
      Files.move(
        self,
        self.resolveSibling(newName),
        StandardCopyOption.REPLACE_EXISTING
      )

    def deleteDirectory(): Unit =
      Files
        .walk(self)
        .sorted(Comparator.reverseOrder())
        .forEach(p => Files.delete(p))

    def createFile(
        name: String,
        content: StringBuilder
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
