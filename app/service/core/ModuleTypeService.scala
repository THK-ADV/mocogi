package service.core

import database.repo.core.ModuleTypeRepository
import models.core.ModuleType
import parser.Parser
import parsing.core.ModuleTypeFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleTypeService @Inject() (
    val repo: ModuleTypeRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[ModuleType] {
  override def fileParser: Parser[List[ModuleType]] =
    ModuleTypeFileParser.parser()
}
