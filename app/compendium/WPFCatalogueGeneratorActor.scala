package compendium

import akka.actor.{Actor, ActorRef, Props}
import compendium.WPFCatalogueGeneratorActor.Generate
import database.repo.WPFRepository
import database.view.StudyProgramViewRepository
import git.api.GitAvailabilityChecker
import models.core.Identity
import models.{FullPoId, Module, Semester, StudyProgramView}
import ops.EitherOps.EStringThrowOps
import ops.FileOps.FileOps0
import ops.LoggerOps
import play.api.Logging

import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

@Singleton
final class WPFCatalogueGeneratorActor(actor: ActorRef) {
  def generate(semester: Semester): Unit =
    actor ! Generate(semester)
}

object WPFCatalogueGeneratorActor {
  def props(
      gitAvailabilityChecker: GitAvailabilityChecker,
      wpfRepo: WPFRepository,
      studyProgramViewRepo: StudyProgramViewRepository,
      ctx: ExecutionContext,
      tmpFolderPath: String,
      wpfCatalogueFolderPath: String
  ) = Props(
    new Impl(
      gitAvailabilityChecker,
      wpfRepo,
      studyProgramViewRepo,
      tmpFolderPath,
      wpfCatalogueFolderPath,
      ctx
    )
  )

  private case class Generate(semester: Semester) extends AnyVal

  private class Impl(
      gitAvailabilityChecker: GitAvailabilityChecker,
      wpfRepo: WPFRepository,
      studyProgramViewRepo: StudyProgramViewRepository,
      tmpFolderPath: String,
      wpfCatalogueFolderPath: String,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging
      with LoggerOps {

    private def tmpFolder = Paths.get(tmpFolderPath)

    private def wpfCatalogueFolder = Paths.get(wpfCatalogueFolderPath)

    private def fillHeaderWithAllStudyPrograms(
        csv: StringBuilder,
        sps: Seq[StudyProgramView]
    ) = {
      sps.foreach(p => {
        csv.append(
          p.specialization match {
            case Some(spec) =>
              s",${p.studyProgram.deLabel}-${spec.deLabel}-${p.poVersion}-${p.degree.deLabel}"
            case None =>
              s",${p.studyProgram.deLabel}-${p.poVersion}-${p.degree.deLabel}"
          }
        )
      })
      csv += '\n'
    }

    private def fillContent(
        csv: StringBuilder,
        sps: Seq[StudyProgramView],
        electiveModules: Iterable[(Module, Seq[Identity], Seq[FullPoId])]
    ): Unit =
      electiveModules.toVector
        .sortBy(_._1.title)
        .foreach { case (module, identities, pos) =>
          val moduleManagement = identities.map(_.fullName).mkString(", ")
          val matchedStudyPrograms = sps
            .map(sp => if (pos.contains(sp.fullPoId)) "X" else "")
            .mkString(",")
          csv.append(
            s"${module.title},${module.abbrev},\"$moduleManagement\",$matchedStudyPrograms\n"
          )
        }

    // TODO discuss usage of semester since recommended semester does not consider whether a study program starts in summer or winter
    // TODO split by teaching unit
    private def generate(semester: Semester): Future[Path] = {
      val csv = new StringBuilder()
      csv.append("Modulname,Modulabkürzung,Modulverantwortliche")
      for {
        _ <- gitAvailabilityChecker.checkAvailability()
        studyPrograms <- studyProgramViewRepo
          .all()
          .map(_.sortBy(a => (a.degree.id, a.fullPoId, a.poVersion)))
        electiveModules <- wpfRepo.all()
        file <- {
          fillHeaderWithAllStudyPrograms(csv, studyPrograms)
          fillContent(csv, studyPrograms, electiveModules)
          createCSVFile(semester.id, csv)
            .flatMap(moveFileToFolder)
            .toFuture
        }
      } yield file
    }

    private def createCSVFile(
        name: String,
        content: StringBuilder
    ): Either[String, Path] = tmpFolder.createFile(s"$name.csv", content)

    private def moveFileToFolder(file: Path): Either[String, Path] =
      try {
        Right(
          Files.move(
            file,
            wpfCatalogueFolder.resolve(file.getFileName),
            StandardCopyOption.REPLACE_EXISTING
          )
        )
      } catch {
        case NonFatal(e) => Left(e.getMessage)
      }

    override def receive: Receive = { case Generate(semester) =>
      logger.info("start generating elective module catalog")
      generate(semester) onComplete {
        case Success(csv) => logSuccess(s"created file ${csv.toAbsolutePath}")
        case Failure(e)   => logFailure(e)
      }
    }
  }
}
