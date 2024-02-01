package service.core

import database.repo.ModuleTypeRepository
import models.core.ModuleType
import parsing.core.{FileParser, ModuleTypeFileParser}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait ModuleTypeService extends SimpleYamlService[ModuleType]

@Singleton
final class ModuleTypeServiceImpl @Inject() (
    val repo: ModuleTypeRepository,
    val ctx: ExecutionContext
) extends ModuleTypeService {
  override def fileParser: FileParser[ModuleType] = ModuleTypeFileParser
}
