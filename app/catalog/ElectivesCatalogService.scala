package catalog

import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.ElectivesRepository
import database.view.StudyProgramViewRepository
import models._
import models.core.Identity
import ops.EitherOps.EStringThrowOps
import ops.FileOps.FileOps0
import play.api.Logging

@Singleton
final class ElectivesCatalogService @Inject() (
    electivesRepository: ElectivesRepository,
    studyProgramViewRepo: StudyProgramViewRepository,
    tmpFolderPath: String,
    electivesCatalogOutputFolderPath: String,
    implicit val ctx: ExecutionContext
) extends Logging {

  private def tmpFolder = Paths.get(tmpFolderPath)

  private def electivesCatalogueFolder =
    Paths.get(electivesCatalogOutputFolderPath)

  private def fillHeaderWithAllStudyPrograms(
      csv: StringBuilder,
      sps: Seq[StudyProgramView]
  ) = {
    sps.foreach(p => {
      csv.append(
        p.specialization match {
          case Some(spec) =>
            s",${p.deLabel}-${spec.deLabel}-${p.po.version}-${p.degree.deLabel}"
          case None =>
            s",${p.deLabel}-${p.po.version}-${p.degree.deLabel}"
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
      .foreach {
        case (module, identities, pos) =>
          val moduleManagement = identities.map(_.fullName).mkString(", ")
          val matchedStudyPrograms = sps
            .map(sp => if (pos.contains(sp.fullPoId)) "X" else "")
            .mkString(",")
          if (matchedStudyPrograms.contains("X")) {
            csv.append(
              s"${module.title},${module.abbrev},\"$moduleManagement\",$matchedStudyPrograms\n"
            )
          }
      }

  def create(semester: Semester): Future[List[(ElectivesFile, String)]] = {
    logger.info(s"creating elective catalog for ${semester.id}")
    val studyPrograms = studyProgramViewRepo
      .all()
      .map(_.sortBy(a => (a.degree.id, a.fullPoId, a.po.version)))
    val electiveModules = electivesRepository.all()

    for {
      studyPrograms   <- studyPrograms
      electiveModules <- electiveModules
      file <- Future.sequence(
        studyPrograms
          .groupBy(_.po.id.split("_").head)
          .map {
            case (teachingUnit, studyPrograms) =>
              val csv = new StringBuilder()
              csv.append("Modulname,Modulabkürzung,Modulverantwortliche")
              fillHeaderWithAllStudyPrograms(csv, studyPrograms)
              fillContent(csv, studyPrograms, electiveModules)
              val content = csv.toString()
              tmpFolder
                .createFile(
                  ElectivesFile.fileName(semester, teachingUnit),
                  content
                )
                .flatMap(_.move(electivesCatalogueFolder))
                .map(ElectivesFile(_) -> content)
                .toFuture
          }
          .toList
      )
    } yield file
  }
}
