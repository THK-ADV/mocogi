package service

import models.Module
import ops.EitherOps.EOps
import parsing.types.{Content, ModuleCompendium, ParsedMetadata}
import validator._

object MetadataValidatingService {
  def validateMany(
      existing: Seq[Module],
      parsed: Seq[(Print, ParsedMetadata, Content, Content)]
  ): Either[Seq[PipelineError], Seq[(Print, ModuleCompendium)]] = {
    val parsedModules =
      parsed.map(a => models.Module(a._2.id, a._2.title, a._2.abbrev))
    val modules = existing ++ parsedModules
    val validator =
      MetadataValidator.validate(30, id => modules.find(_.id == id)) _
    val (errs, moduleCompendiums) =
      parsed.partitionMap { case (print, parsedMetadata, de, en) =>
        validator(parsedMetadata).bimap(
          errs =>
            PipelineError.Validator(
              ValidationError(errs),
              Some(parsedMetadata.id)
            ),
          metadata => (print, ModuleCompendium(metadata, de, en))
        )
      }
    Either.cond(errs.isEmpty, moduleCompendiums, errs)
  }

  def validate(
      existing: Seq[Module],
      metadata: ParsedMetadata
  ): Validation[Metadata] = {
    val parsedModule = models.Module(metadata.id, metadata.title, metadata.abbrev)
    val modules = existing.+:(parsedModule)
    val validator =
      MetadataValidator.validate(30, id => modules.find(_.id == id)) _
    validator(metadata)
  }
}
