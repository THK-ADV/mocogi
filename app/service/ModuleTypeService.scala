package service

import basedata.ModuleType
import database.repo.ModuleTypeRepository
import parsing.base.ModuleTypeFileParser

import javax.inject.{Inject, Singleton}

trait ModuleTypeService extends YamlService[ModuleType, ModuleType]

@Singleton
final class ModuleTypeServiceImpl @Inject() (
    val repo: ModuleTypeRepository,
    val parser: ModuleTypeFileParser
) extends ModuleTypeService {
  override def toInput(output: ModuleType) = output
}
