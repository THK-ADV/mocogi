package database.repo

import basedata.{RestrictedAdmission, StudyProgram}
import database.table._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.time.LocalDate
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

case class StudyProgramOutput(
    abbrev: String,
    deLabel: String,
    enLabel: String,
    internalAbbreviation: String,
    externalAbbreviation: String,
    deUrl: String,
    enUrl: String,
    grade: String,
    programDirector: String,
    accreditationUntil: LocalDate,
    restrictedAdmission: RestrictedAdmission,
    studyForm: List[UUID],
    language: List[String],
    seasons: List[String],
    campus: List[String],
    deDescription: String,
    deNote: String,
    enDescription: String,
    enNote: String
)

@Singleton
class StudyProgramRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[StudyProgramTable]

  protected val studyFormTableQuery = TableQuery[StudyFormTable]

  protected val studyFormScopeTableQuery = TableQuery[StudyFormScopeTable]

  protected val studyProgramLanguageTableQuery =
    TableQuery[StudyProgramLanguageTable]

  protected val studyProgramSeasonTableQuery =
    TableQuery[StudyProgramSeasonTable]

  protected val studyProgramLocationTableQuery =
    TableQuery[StudyProgramLocationTable]

  def all(): Future[Seq[StudyProgramOutput]] =
    retrieve(tableQuery)

  def allIds(): Future[Seq[String]] =
    db.run(tableQuery.map(_.abbrev).result)

  protected def retrieve(
      query: Query[StudyProgramTable, StudyProgramDbEntry, Seq]
  ) = {
    val res = query
      .joinLeft(studyFormTableQuery)
      .on(_.abbrev === _.studyProgram)
      .joinLeft(studyProgramLanguageTableQuery)
      .on(_._1.abbrev === _.studyProgram)
      .joinLeft(studyProgramSeasonTableQuery)
      .on(_._1._1.abbrev === _.studyProgram)
      .joinLeft(studyProgramLocationTableQuery)
      .on(_._1._1._1.abbrev === _.studyProgram)

    db.run(
      res.result.map(_.groupBy(_._1._1._1._1).map { case (sp, sf) =>
        val studyForms = mutable.HashSet[UUID]()
        val studyProgramLanguages = mutable.HashSet[String]()
        val studyProgramSeasons = mutable.HashSet[String]()
        val studyProgramLocations = mutable.HashSet[String]()

        sf.foreach { case ((((_, f), lang), s), loc) =>
          f.foreach(studyForms += _.id)
          lang.foreach(studyProgramLanguages += _.language)
          s.foreach(studyProgramSeasons += _.season)
          loc.foreach(studyProgramLocations += _.location)
        }

        StudyProgramOutput(
          sp.abbrev,
          sp.deLabel,
          sp.enLabel,
          sp.internalAbbreviation,
          sp.externalAbbreviation,
          sp.deUrl,
          sp.enUrl,
          sp.grade,
          sp.programDirector,
          sp.accreditationUntil,
          sp.restrictedAdmission,
          studyForms.toList,
          studyProgramLanguages.toList,
          studyProgramSeasons.toList,
          studyProgramLocations.toList,
          sp.deDescription,
          sp.deNote,
          sp.enDescription,
          sp.enNote
        )
      }.toSeq)
    )
  }

  def createMany(ls: List[StudyProgram]) = {
    val studyPrograms = ListBuffer[StudyProgramDbEntry]()
    val studyForms = ListBuffer[StudyFormDbEntry]()
    val studyFormScopes = ListBuffer[StudyFormScopeDbEntry]()
    val studyProgramLanguages = ListBuffer[StudyProgramLanguageDbEntry]()
    val studyProgramSeasons = ListBuffer[StudyProgramSeasonDbEntry]()
    val studyProgramLocations = ListBuffer[StudyProgramLocationDbEntry]()

    ls.foreach { sp =>
      studyPrograms += toDbEntry(sp)
      val spId = sp.abbrev
      sp.language.foreach { l =>
        studyProgramLanguages += StudyProgramLanguageDbEntry(l.abbrev, spId)
      }
      sp.seasons.foreach { s =>
        studyProgramSeasons += StudyProgramSeasonDbEntry(s.abbrev, spId)
      }
      sp.campus.foreach { c =>
        studyProgramLocations += StudyProgramLocationDbEntry(c.abbrev, spId)
      }
      sp.studyForm.foreach { sf =>
        val sfId = UUID.randomUUID()
        studyForms += StudyFormDbEntry(
          sfId,
          spId,
          sf.kind.abbrev,
          sf.workloadPerEcts
        )
        sf.scope.foreach { sfs =>
          studyFormScopes += StudyFormScopeDbEntry(
            UUID.randomUUID(),
            sfId,
            sfs.programDuration,
            sfs.totalEcts,
            sfs.deReason,
            sfs.enReason
          )
        }
      }
    }

    val action = for {
      _ <- tableQuery ++= studyPrograms
      _ <- studyFormTableQuery ++= studyForms
      _ <- studyFormScopeTableQuery ++= studyFormScopes
      _ <- studyProgramLanguageTableQuery ++= studyProgramLanguages
      _ <- studyProgramSeasonTableQuery ++= studyProgramSeasons
      _ <- studyProgramLocationTableQuery ++= studyProgramLocations
    } yield studyPrograms.toList

    db.run(action.transactionally)
  }

  private def toDbEntry(sp: StudyProgram): StudyProgramDbEntry =
    StudyProgramDbEntry(
      sp.abbrev,
      sp.deLabel,
      sp.enLabel,
      sp.internalAbbreviation,
      sp.externalAbbreviation,
      sp.deUrl,
      sp.enUrl,
      sp.grade.abbrev,
      sp.programDirector.id,
      sp.accreditationUntil,
      sp.restrictedAdmission,
      sp.deDescription,
      sp.deNote,
      sp.enDescription,
      sp.enNote
    )
}
