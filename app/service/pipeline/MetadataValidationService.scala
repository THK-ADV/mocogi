package service.pipeline

import models.Metadata
import models.ModuleCore
import ops.bimap
import parsing.types.Module
import parsing.types.ModuleContent
import parsing.types.ParsedMetadata
import validation.MetadataValidator
import validation.Validation

private[pipeline] object MetadataValidationService {

  def validateMany(
      context: ValidationContext,
      parsed: Seq[(Print, ParsedMetadata, ModuleContent, ModuleContent)]
  ): Either[Seq[PipelineError], Seq[(Print, Module)]] = {
    val parsedModules =
      parsed.map(a => a._2.id -> ModuleCore(a._2.id, a._2.title, a._2.abbrev))
    val modulesById = context.modulesById ++ parsedModules
    val validations = MetadataValidator.validateMany(
      parsed.map(_._2),
      modulesById.get,
      context.relations
    )
    val (errs, validated) =
      parsed.zip(validations).partitionMap {
        case ((print, parsedMetadata, de, en), validation) =>
          validation.bimap(
            errs => PipelineError.validator(errs, Some(parsedMetadata.id)),
            metadata => (print, Module(metadata, de, en))
          )
      }
    Either.cond(errs.isEmpty, validated, errs)
  }

  def validate(
      context: ValidationContext,
      metadata: ParsedMetadata
  ): Validation[Metadata] = {
    val parsedModule =
      ModuleCore(metadata.id, metadata.title, metadata.abbrev)
    val modules   = context.modulesById.updated(metadata.id, parsedModule)
    val validator = MetadataValidator.validate(modules.get, context.relations)
    validator(metadata)
  }
}
