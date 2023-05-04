package ops

import java.io.File
import scala.util.Try

object FileOps {
  def getFile(path: String): Try[File] =
    Try {
      val file = new File(path)
      if (file.exists()) file
      else throw new Throwable(s"file not found: $path")
    }
}
