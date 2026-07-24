package service.pipeline

import java.util.UUID

import models.ModuleCore
import validation.ModuleRelationGraph

private[pipeline] final case class ValidationContext(
    modulesById: Map[UUID, ModuleCore],
    relations: ModuleRelationGraph
)
