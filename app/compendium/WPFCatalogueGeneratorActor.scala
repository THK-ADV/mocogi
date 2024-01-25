package compendium

import akka.actor.{Actor, ActorRef, Props}
import compendium.WPFCatalogueGeneratorActor.Generate
import database.repo.{PORepository, WPFRepository}
import git.api.GitAvailabilityChecker
import models.Semester
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
      poRepo: PORepository,
      ctx: ExecutionContext,
      tmpFolderPath: String,
      wpfCatalogueFolderPath: String
  ) = Props(
    new Impl(
      gitAvailabilityChecker,
      wpfRepo,
      poRepo,
      tmpFolderPath,
      wpfCatalogueFolderPath,
      ctx
    )
  )

  private case class Generate(semester: Semester) extends AnyVal

  private class Impl(
      gitAvailabilityChecker: GitAvailabilityChecker,
      wpfRepo: WPFRepository,
      poRepo: PORepository,
      tmpFolderPath: String,
      wpfCatalogueFolderPath: String,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging
      with LoggerOps {

    private def tmpFolder = Paths.get(tmpFolderPath)

    private def wpfCatalogueFolder = Paths.get(wpfCatalogueFolderPath)

    // TODO discuss usage of semester since recommended semester does not consider whether a study program starts in summer or winter
    private def generate(semester: Semester): Future[Path] = {
      val csv = new StringBuilder()
      csv.append("Modulname,Modulabkürzung,Modulverantwortliche")
      for {
        _ <- gitAvailabilityChecker.checkAvailability()
        allPos <- poRepo
          .allValidShort()
          .map(_.sortBy(a => (a.studyProgram.grade, a.fullAbbrev, a.version)))
        entries <- wpfRepo.all()
        file <- {
          allPos.foreach(p => {
            csv.append(
              p.specialization match {
                case Some(spec) =>
                  s",${p.studyProgram.deLabel}-${spec.label}-${p.version}-${p.studyProgram.grade.deLabel}"
                case None =>
                  s",${p.studyProgram.deLabel}-${p.version}-${p.studyProgram.grade.deLabel}"
              }
            )
          })
          csv += '\n'
          entries.foreach { case (module, person, pos) =>
            val po = allPos
              .map(p =>
                if (pos.exists(_.fullAbbrev == p.fullAbbrev)) "X" else ""
              )
              .mkString(",")
            csv.append(
              s"${module.title},${module.abbrev},${person.fullName},$po\n"
            )
          }
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
      generate(semester) onComplete {
        case Success(csv) => logSuccess(s"created file ${csv.toAbsolutePath}")
        case Failure(e)   => logFailure(e)
      }
    }
  }
}
