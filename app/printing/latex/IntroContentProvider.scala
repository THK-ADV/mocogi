package printing.latex

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.stream.Collectors

import scala.util.control.NonFatal

import models.FullPoId
import ops.FileOps.FileOps0
import play.api.Logging
import printing.LanguageOps

final class IntroContentProvider(dir: Path, fullPoId: FullPoId, mcIntroPath: String) extends Logging {

  private def copyIntoDir(): Option[Path] =
    getTexFile.map { texPath =>
      val srcDir = texPath.getParent
      Files.walk(srcDir).forEach { src =>
        if src != srcDir then {
          val dest = Paths.get(dir.toString, src.toString.substring(srcDir.toString.length))
          if Files.isDirectory(dest) then dest.deleteDirectory()
          Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING)
        }
      }
      texPath.getFileName
    }

  def createIntroContent(): Option[IntroContent] =
    copyIntoDir().map { textFileName => (pLang, _, builder) =>
      {
        builder.append(s"\\chapter{${pLang.prologHeadline}}\n")
        builder.append("\\newpage\n")
        builder.append(s"\\include{$textFileName}\n")
      }
    }

  private def getTexFile: Option[Path] = {
    val path = Paths.get(mcIntroPath, fullPoId.id)
    if Files.notExists(path) then {
      logger.info(s"no intro found for $fullPoId. Path: $path")
      None
    } else
      try {
        val files = Files
          .find(
            path,
            1,
            (path, _) => {
              val res = path.getFileName.toString.split('.')
              res.length == 2 && res.last.equalsIgnoreCase("tex")
            }
          )
          .collect(Collectors.toList)
        files.size() match
          case 0 => None
          case 1 =>
            val file = files.get(0)
            logger.info(s"found intro file: $file")
            Some(file)
          case _ =>
            logger.error(s"expected at most one one tex file, but found $files for id ${fullPoId.id}")
            None
      } catch {
        case NonFatal(e) =>
          logger.error(s"failed to find tex file for ${fullPoId.id}", e)
          None
      }
  }
}
