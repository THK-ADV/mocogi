package service.core

import database.repo.core.ModuleTypeRepository
import models.core.ModuleType
import parsing.core.{FileParser, ModuleTypeFileParser}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleTypeService @Inject() (
    val repo: ModuleTypeRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[ModuleType] {
  override def fileParser: FileParser[ModuleType] = ModuleTypeFileParser
}
