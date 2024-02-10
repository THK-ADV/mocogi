package catalog

import database.repo.ElectivesRepository
import database.view.StudyProgramViewRepository
import git.api.GitAvailabilityChecker
import models._
import models.core.Identity
import ops.EitherOps.EStringThrowOps
import ops.FileOps.FileOps0
import ops.LoggerOps
import play.api.Logging

import java.nio.file.{Path, Paths}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ElectivesCatalogService @Inject() (
    gitAvailabilityChecker: GitAvailabilityChecker,
    electivesRepository: ElectivesRepository,
    studyProgramViewRepo: StudyProgramViewRepository,
    tmpFolderPath: String,
    electivesCatalogFolderPath: String,
    gitFolderPath: String,
    implicit val ctx: ExecutionContext
) extends Logging {

  private def tmpFolder = Paths.get(tmpFolderPath)

  private def electivesCatalogueFolder = Paths.get(electivesCatalogFolderPath)

  private def gitFolder = Paths.get(gitFolderPath)

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
      electiveModules: Iterable[(ModuleCore, Seq[Identity], Seq[FullPoId])]
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
  def create(semester: Semester): Future[Path] = {
    logger.info(s"creating elective catalog for ${semester.id}")
    val csv = new StringBuilder()
    csv.append("Modulname,Modulabkürzung,Modulverantwortliche")
    for {
      _ <- gitAvailabilityChecker.checkAvailability()
      studyPrograms <- studyProgramViewRepo
        .all()
        .map(_.sortBy(a => (a.degree.id, a.fullPoId, a.poVersion)))
      electiveModules <- electivesRepository.all()
      file <- {
        fillHeaderWithAllStudyPrograms(csv, studyPrograms)
        fillContent(csv, studyPrograms, electiveModules)
        createCSVFile(semester.id, csv)
          .flatMap(_.move(electivesCatalogueFolder))
          .flatMap(_.copy(gitFolder))
          .toFuture
      }
    } yield file
  }

  private def createCSVFile(
      name: String,
      content: StringBuilder
  ): Either[String, Path] = tmpFolder.createFile(s"$name.csv", content)
}
