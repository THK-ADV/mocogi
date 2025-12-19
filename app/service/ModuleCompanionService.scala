package service

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.ModuleCompanionRepository
import git.api.GitFileService
import git.GitFilePath
import io.circe.Json
import models.ModuleCompanion
import play.api.libs.json.*
import play.api.Logging

@Singleton
final class ModuleCompanionService @Inject() (
    repo: ModuleCompanionRepository,
    fileService: GitFileService,
    implicit val ctx: ExecutionContext
) extends Logging {
  def allFromModules(modules: Seq[UUID]) = {
    def parseFrontMatter(input: String, mc: ModuleCompanion) = {
      assume(input.startsWith("---"))
      val str  = input.drop(3)
      val end  = str.indexOf("---")
      val yaml = str.slice(0, end).trim
      io.circe.yaml.parser.parse(yaml) match
        case Left(err) =>
          logger.error(s"unable to parse yaml from module ${mc.module} and po ${mc.po}", err)
          None
        case Right(js) =>
          Some(circeToPlay(js))
    }
    val config = fileService.config
    for
      companions <- repo.allFromModules(modules)
      companionFiles <- Future.sequence(
        companions.map(companion =>
          fileService
            .download(GitFilePath.moduleCompanionPath(companion.module, companion.po)(config), config.draftBranch)
            .map(content => companion -> content.flatMap { (c, _) => parseFrontMatter(c.value, companion) })
        )
      )
    yield companionFiles
  }

  private def circeToPlay(circeJson: io.circe.Json): JsValue =
    circeJson.fold(
      JsNull,
      b => JsBoolean(b),
      n => JsNumber(n.toDouble),
      s => JsString(s),
      arr => JsArray(arr.map(circeToPlay)),
      obj => JsObject(obj.toMap.map((k, v) => (k, circeToPlay(v))))
    )
}
