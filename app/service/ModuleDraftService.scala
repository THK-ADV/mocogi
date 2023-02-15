package service

import controllers.formats.{
  ModuleCompendiumFormat,
  ModuleCompendiumProtocolFormat,
  PipelineErrorFormat
}
import database.repo.{ModuleDraftRepository, UserBranchRepository}
import models._
import parsing.metadata.VersionScheme
import parsing.types._
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import printing.yaml.ModuleCompendiumYamlPrinter
import validator.{MetadataValidator, Module, ValidationError}

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class ModuleDraftService @Inject() (
    private val moduleDraftRepository: ModuleDraftRepository,
    private val userBranchRepository: UserBranchRepository,
    private val moduleCompendiumService: ModuleCompendiumService,
    private val moduleCompendiumPrinter: ModuleCompendiumYamlPrinter,
    private val metadataParsingService: MetadataParsingService,
    private val contentParsingService: ContentParsingService,
    private implicit val ctx: ExecutionContext
) extends ModuleCompendiumProtocolFormat
    with PipelineErrorFormat
    with ModuleCompendiumFormat {
  import ops.EitherOps._
  import ops.JsResultOps._

  type Result[A] = Future[Either[Seq[PipelineError], Seq[A]]]

  private type PrintingResult = Result[(UUID, Print)]
  private type ParsingResult = Result[(Print, ParsedMetadata, Content, Content)]
  private type ValidationResult = Result[(Print, ModuleCompendium)]

  def allFromBranch(branch: String): Future[Seq[ModuleDraft]] =
    moduleDraftRepository.allFromBranch(branch)

  def delete(userBranch: UserBranch): Future[Int] =
    moduleDraftRepository.delete(userBranch.branch)

  def createOrUpdate(
      module: Option[UUID],
      data: String,
      branch: String
  ): Future[ModuleDraft] = {
    def go() = module match {
      case Some(id) =>
        moduleDraftRepository.get(id).flatMap {
          case Some(draft) =>
            moduleDraftRepository
              .update(draft.copy(data = data, lastModified = LocalDateTime.now))
          case None =>
            moduleDraftRepository.create(
              ModuleDraft(
                id,
                data,
                branch,
                ModuleDraftStatus.Modified,
                LocalDateTime.now(),
                None
              )
            )
        }
      case None =>
        moduleDraftRepository.create(
          ModuleDraft(
            UUID.randomUUID,
            data,
            branch,
            ModuleDraftStatus.Added,
            LocalDateTime.now(),
            None
          )
        )
    }

    for {
      branchExists <- userBranchRepository.exists(branch)
      res <-
        if (branchExists) go()
        else
          Future.failed(
            new Throwable(s"branch $branch doesn't exist")
          )
    } yield res
  }

  private def print(branch: String): PrintingResult =
    for {
      drafts <- moduleDraftRepository.allFromBranch(branch)
      protocols <- Future.fromTry(parseDrafts(drafts))
      printer = moduleCompendiumPrinter.printer(VersionScheme(1, "s"))
      (errs, prints) = protocols.partitionMap(e =>
        printer
          .print(e, "")
          .bimap(
            PipelineError.Printer(_, e._1),
            p => (e._1, Print(p))
          )
      )
    } yield Either.cond(errs.isEmpty, prints, errs)

  private def parse(inputs: Seq[(UUID, Print)]): ParsingResult =
    metadataParsingService.parseMany(inputs).map { res =>
      val (errs, parses) = res.partitionMap { case (res, rest) =>
        res match {
          case Left((id, err)) => Left(PipelineError.Parser(err, id))
          case Right((print, parsedMetadata)) =>
            contentParsingService.parse(rest.value)._1 match {
              case Left(err) =>
                Left(PipelineError.Parser(err, parsedMetadata.id))
              case Right((de, en)) => Right((print, parsedMetadata, de, en))
            }

        }
      }
      Either.cond(errs.isEmpty, parses, errs)
    }

  private def validate(
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
                parsedMetadata.id
              ),
            metadata => (print, ModuleCompendium(metadata, de, en))
          )
        }
      Either.cond(errs.isEmpty, moduleCompendiums, errs)
    }

  private def persist(
      e: Either[Seq[PipelineError], Seq[(Print, ModuleCompendium)]]
  ): Future[Unit] =
    Future
      .sequence(
        e match {
          case Left(errs) =>
            errs.groupBy(_.metadata).map { case (id, errs) =>
              moduleDraftRepository
                .updateValidation(id, Left(Json.toJson(errs)))
            }
          case Right(mcs) =>
            mcs.map { case (print, mc) =>
              val mcJson = Json.toJson(mc)
              moduleDraftRepository.updateValidation(
                mc.metadata.id,
                Right((mcJson, print))
              )
            }
        }
      )
      .map(_ => ())

  private def continue[A, B](
      e: Either[Seq[PipelineError], Seq[A]],
      f: Seq[A] => Future[Either[Seq[PipelineError], B]]
  ): Future[Either[Seq[PipelineError], B]] =
    e match {
      case Left(errs) =>
        Future.successful(Left(errs))
      case Right(res) =>
        f(res)
    }

  def validateDrafts(branch: String): ValidationResult =
    for {
      prints <- print(branch)
      parses <- continue(prints, parse)
      validates <- continue(parses, validate)
      _ <- persist(validates)
    } yield validates

  def validDrafts(branch: String): Future[Seq[ValidModuleDraft]] =
    for {
      drafts <- moduleDraftRepository.allFromBranch(branch)
    } yield drafts.map { d =>
      d.validation match {
        case Some(Right((json, print))) =>
          ValidModuleDraft(
            d.module,
            d.status,
            d.lastModified,
            json,
            print
          )
        case _ =>
          throw new Throwable(
            s"expected branch $branch to only contain valid drafts"
          )
      }
    }

  def parseModuleCompendium(json: JsValue): Try[ModuleCompendium] =
    Json.fromJson[ModuleCompendium](json).toTry

  private def parseDrafts(
      drafts: Seq[ModuleDraft]
  ): Try[Seq[(UUID, ModuleCompendiumProtocol)]] =
    Try(drafts.map { draft =>
      moduleCompendiumProtocolFormat.reads(Json.parse(draft.data)) match {
        case JsSuccess(value, _) => (draft.module, value)
        case JsError(errors)     => throw new Throwable(errors.mkString("\n"))
      }
    })

}
