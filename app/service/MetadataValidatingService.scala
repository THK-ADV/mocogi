package service

import models.Metadata
import models.ModuleCore
import models.ModuleWorkload
import ops.EitherOps.EOps
import parsing.types.Module
import parsing.types.ModuleContent
import parsing.types.ModuleECTS
import parsing.types.ParsedMetadata
import parsing.types.ParsedWorkload
import validation.*

object MetadataValidatingService {

  private val ectsFactor = 30

  def validateWorkload(workload: ParsedWorkload, moduleECTS: Double): Validation[ModuleWorkload] =
    MetadataValidator.workloadValidator(ectsFactor).validate((workload, ModuleECTS(moduleECTS, Nil)))

  def validateMany(
      existing: Seq[ModuleCore],
      parsed: Seq[(Print, ParsedMetadata, ModuleContent, ModuleContent)]
  ): Either[Seq[PipelineError], Seq[(Print, Module)]] = {
    val parsedModules =
      parsed.map(a => ModuleCore(a._2.id, a._2.title, a._2.abbrev))
    val modules = existing ++ parsedModules
    val validator =
      MetadataValidator.validate(ectsFactor, id => modules.find(_.id == id)) _
    val (errs, validated) =
      parsed.partitionMap {
        case (print, parsedMetadata, de, en) =>
          validator(parsedMetadata).bimap(
            errs =>
              PipelineError.Validator(
                ValidationError(errs),
                Some(parsedMetadata.id)
              ),
            metadata => (print, Module(metadata, de, en))
          )
      }
    Either.cond(errs.isEmpty, validated, errs)
  }

  def validate(
      existing: Seq[ModuleCore],
      metadata: ParsedMetadata
  ): Validation[Metadata] = {
    val parsedModule =
      ModuleCore(metadata.id, metadata.title, metadata.abbrev)
    val modules = existing.+:(parsedModule)
    val validator =
      MetadataValidator.validate(ectsFactor, id => modules.find(_.id == id)) _
    validator(metadata)
  }
}
