package service.core

import database.repo.ModuleTypeRepository
import models.core.ModuleType
import parsing.core.ModuleTypeFileParser

import javax.inject.{Inject, Singleton}

trait ModuleTypeService extends SimpleYamlService[ModuleType]

@Singleton
final class ModuleTypeServiceImpl @Inject() (
    val repo: ModuleTypeRepository,
    val parser: ModuleTypeFileParser
) extends ModuleTypeService
