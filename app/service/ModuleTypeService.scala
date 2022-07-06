package service

import database.repo.ModuleTypeRepository
import parsing.metadata.file.ModuleTypeFileParser
import parsing.types.ModuleType

import javax.inject.{Inject, Singleton}

trait ModuleTypeService extends YamlService[ModuleType]

@Singleton
final class ModuleTypeServiceImpl @Inject() (
    val repo: ModuleTypeRepository,
    val parser: ModuleTypeFileParser
) extends ModuleTypeService
