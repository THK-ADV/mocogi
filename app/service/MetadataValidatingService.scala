package service

import com.google.inject.{Inject, Singleton}
import ops.EitherOps.EOps
import parsing.types.{Content, ModuleCompendium, ParsedMetadata}
import validator.{Metadata, MetadataValidator, Module, Validation, ValidationError}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MetadataValidatingService @Inject() (
    private val moduleCompendiumService: ModuleCompendiumService,
    private implicit val ctx: ExecutionContext
) {
  def validateMany(
      parsed: Seq[(Print, ParsedMetadata, Content, Content)]
  ): ValidationResult =
    moduleCompendiumService.allIdsAndAbbrevs().map { existing =>
      val existingModules = existing.map(Module.tupled)
      val parsedModules = parsed.map(a => Module(a._2.id, a._2.abbrev))
      val modules = existingModules ++ parsedModules
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

  def validate(metadata: ParsedMetadata): Future[Validation[Metadata]] =
    moduleCompendiumService.allIdsAndAbbrevs().map { existing =>
      val existingModules = existing.map(Module.tupled)
      val parsedModule = Module(metadata.id, metadata.abbrev)
      val modules = existingModules.+:(parsedModule)
      val validator =
        MetadataValidator.validate(30, id => modules.find(_.id == id)) _
      validator(metadata)
    }
}
