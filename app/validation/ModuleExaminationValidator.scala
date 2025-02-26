package validation

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.CampusId
import database.table.core.IdentityTable
import database.table.ModuleAssessmentMethodDbEntry
import database.table.ModuleAssessmentMethodTable
import database.table.ModuleResponsibilityTable
import database.table.ModuleTable
import git.api.GitDiffApiService
import git.api.GitFileDownloadService
import git.GitConfig
import models.AssessmentMethodType
import models.ModuleAssessmentMethodEntryProtocol
import models.ModuleAssessmentMethodsProtocol
import models.ModuleCore
import parsing.metadata.ModuleAssessmentMethodParser
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

final class ModuleExaminationValidator @Inject() (
    diffApiService: GitDiffApiService,
    downloadService: GitFileDownloadService,
    val dbConfigProvider: DatabaseConfigProvider,
    private implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api.*

  private given gitConfig: GitConfig = diffApiService.config

  // TODO descriptions according to RPO
  // TODO precondition?

  private def validExaminations = Vector(
    "written-exam",                      // Klausurarbeit | written examination
    "written-exam-answer-choice-method", // Schriftliche Prüfung im Antwortwahlverfahren | written multiple-choice examination
    "oral-exam",                         // Mündliche Prüfung | oral examination
    "home-assignment",                   // Hausarbeit | term paper
    "open-book-exam",                    // Open-Book-Ausarbeitung | open book examination
    "project",                           // Projektarbeit | project
    "portfolio",                         // Lernportfolio | learning portfolio
    "practical-report",                  // Praktikumsbericht | lab report
    "oral-contribution",                 // Mündlicher Beitrag | oral reports
    "certificate-achievement",           // Testat/Zwischentestat | certificate of achievement/interim certificate of achievement
    "performance-assessment",            // Performanzprüfung | performance assessment
    "role-play",                         // Rollenspiel | role play
    "admission-colloquium",              // Zugangskolloquium | admission colloquium
    "specimen",                          // Präparat | specimen
  )

  private def recommendation = Map(
    "project-documentation"                -> Vector("project"),
    "thesis"                               -> Vector("project", "home-assignment"),
    "single-choice"                        -> Vector("written-exam-answer-choice-method"),
    "certificate"                          -> Vector("certificate-achievement"),
    "written-oral-presentation-of-project" -> Vector("oral-contribution"),
    "case-study"                           -> Vector("home-assignment"),
    "expert-talk"                          -> Vector("oral-contribution"),
    "paper"                                -> Vector("home-assignment"),
    "assignment"                           -> Vector("home-assignment", "project"),
    "verbal-cooperation"                   -> Vector("oral-contribution"),
    "presentation"                         -> Vector("oral-contribution"),
  )

  private def invalidExaminations = Vector(
    "presentation",                         // Referat
    "project-documentation",                // Projektdokumentation
    "reflection-report",                    // Reflektionsbericht
    "practical-semester-report",            // Praxissemesterbericht
    "practical",                            // Praktikum
    "test",                                 // Schriftlicher Test
    "thesis",                               // Schriftliche Ausarbeitung
    "abstract",                             // Abstract
    "e-assessment",                         // Hausarbeit über Ilias E-Assessment
    "single-choice",                        // Single-Choice Klausur
    "multiple-choice",                      // Multiple-Choice-Tests mit Punkten für die Klausur
    "certificate",                          // Leistungsnachweis
    "continous-report",                     // Berichte
    "attendance",                           // Anwesenheit
    "written-oral-presentation-of-project", // Schriftliche und mündliche Präsentation der Ergebnisse der Projektarbeit
    "practical-language-tasks-in-seminar",  // Verschiedene sprachpraktische Aufgaben im Seminar
    "case-study",                           // Fallstudie
    "online-meeting",                       // Planung und Durchführung einer Onlineveranstaltung
    "expert-talk",                          // Fachgespräch
    "paper",                                // Wissenschaftliches Papier
    "postersession",                        // Poster-Session
    "assignment",                           // Semesterbegleitenden Ausarbeitungen
    "verbal-cooperation",                   // Mündliche Mitarbeit
    "businessplan",                         // Businessplan
    "progress-check"                        // Bestandene Lernfortschrittskontrolle
  )

  def getAllInvalidModuleExams: Future[List[(ModuleCore, List[String], Seq[Either[String, String]])]] = {
    val identities = getAllIdentities()
    val downloads  = getFromPreview

    for
      identities <- identities
      downloads  <- downloads.map(_.collect { case Some(p) => p })
      db         <- getFromDb(downloads.map(_._1.id))
      all = downloads.appendedAll(db)
    yield {
      assert(all.distinctBy(_._1.id).map(_._1.id).size == all.map(_._1.id).size)
      all.foldLeft(List.empty[(ModuleCore, List[String], Seq[Either[String, String]])]) {
        case (acc, a) =>
          val all     = a._2.mandatory ::: a._2.optional
          val invalid = all.collect { case a if invalidExaminations.contains(a.method) => a.method }
          if invalid.isEmpty then acc
          else
            val mails = identities.collect {
              case i if a._3.contains(i._1) =>
                i._3 match
                  case Some(value) => Right(CampusId(value).toMailAddress)
                  case None        => Left(i._1)
            }
            (a._1, invalid, mails) :: acc
      }
    }
  }

  private def getAllIdentities(): Future[Seq[(String, String, Option[String])]] =
    db.run(TableQuery[IdentityTable].map(a => (a.id, a.kind, a.campusId)).result)

  private def getFromDb(
      without: Vector[UUID]
  ): Future[Vector[(ModuleCore, ModuleAssessmentMethodsProtocol, List[String])]] =
    db.run(
      TableQuery[ModuleAssessmentMethodTable]
        .filterNot(_.module.inSet(without))
        .join(TableQuery[ModuleTable].map(a => (a.id, a.title, a.abbrev)))
        .on(_.module === _._1)
        .join(TableQuery[ModuleResponsibilityTable].filter(_.isModuleManager))
        .on(_._2._1 === _.module)
        .result
        .map(
          _.groupBy(_._1._2)
            .map {
              case (module, xs) =>
                val (mandatory, elective) = xs.partitionMap { x =>
                  val e: ModuleAssessmentMethodDbEntry = x._1._1
                  e.assessmentMethodType match
                    case AssessmentMethodType.Mandatory =>
                      Left(ModuleAssessmentMethodEntryProtocol(e.assessmentMethod, e.percentage, Nil))
                    case AssessmentMethodType.Optional =>
                      Right(ModuleAssessmentMethodEntryProtocol(e.assessmentMethod, e.percentage, Nil))
                }
                (
                  ModuleCore(module._1, module._2, module._3),
                  ModuleAssessmentMethodsProtocol(mandatory.toList, elective.toList),
                  xs.map(_._2.identity).toList
                )
            }
            .toVector
        )
    )

  private def getFromPreview: Future[Vector[Option[(ModuleCore, ModuleAssessmentMethodsProtocol, List[String])]]] =
    diffApiService
      .compare(gitConfig.mainBranch, gitConfig.draftBranch)
      .flatMap { diffs =>
        val downloads = diffs
          .collect {
            case d
                if d.path.isModule ||
                  d.diff.contains(ModuleAssessmentMethodParser.mandatoryKey) ||
                  d.diff.contains(ModuleAssessmentMethodParser.electiveKey) =>
              downloadService
                .downloadModuleFromPreviewBranch(d.path.moduleId.get)
                .map(_.map {
                  case (p, _) =>
                    (
                      ModuleCore(p.id.get, p.metadata.title, p.metadata.abbrev),
                      p.metadata.assessmentMethods,
                      p.metadata.moduleManagement.toList
                    )
                })
          }
        Future.sequence(downloads).map(_.toVector)
      }
}
