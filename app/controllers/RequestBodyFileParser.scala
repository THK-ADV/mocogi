package controllers

import com.google.common.base.Charsets
import com.google.common.io.Files
import play.api.libs.Files.TemporaryFile
import play.api.mvc.Request

import scala.util.Try

trait RequestBodyFileParser {
  def parseFileContent(implicit r: Request[TemporaryFile]): Try[String] =
    Try(Files.asCharSource(r.body.path.toFile, Charsets.UTF_8).read())
}
