package ops

import java.io.File
import scala.util.{Failure, Success, Try}

object FileOps {
  def getFile(path: String): Try[File] = {
    val file = new File(path)
    if (file.exists()) {
      Success(file)
    } else {
      Failure(throw new Throwable(s"file not found: $path"))
    }
  }
}
