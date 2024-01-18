package ops

import java.nio.file.{Files, Path, StandardCopyOption}
import java.util.Comparator

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
  }
}
