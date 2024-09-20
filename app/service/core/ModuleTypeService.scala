package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.core.ModuleTypeRepository
import models.core.ModuleType
import parser.Parser
import parsing.core.ModuleTypeFileParser

@Singleton
final class ModuleTypeService @Inject() (
    val repo: ModuleTypeRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[ModuleType] {
  override def fileParser: Parser[List[ModuleType]] =
    ModuleTypeFileParser.parser()
}
