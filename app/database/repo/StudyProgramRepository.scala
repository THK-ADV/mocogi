package database.repo

import database.table._
import models.core.{RestrictedAdmission, StudyProgram}
import models.{StudyProgramShort, UniversityRole}
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
    programDirectors: List[String],
    examDirectors: List[String],
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

  protected val studyProgramPersonTableQuery =
    TableQuery[StudyProgramPersonTable]

  def all(): Future[Seq[StudyProgramOutput]] =
    retrieve(tableQuery)

  def allShort(): Future[Seq[StudyProgramShort]] = {
    val query = for {
      q <- tableQuery
      g <- q.gradeFk
    } yield (q.abbrev, q.deLabel, q.enLabel, g)
    db.run(query.result.map(_.map(StudyProgramShort.apply)))
  }

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
      .joinLeft(studyProgramPersonTableQuery)
      .on(_._1._1._1._1.abbrev === _.studyProgram)

    db.run(
      res.result.map(_.groupBy(_._1._1._1._1._1).map { case (sp, sf) =>
        val studyForms = mutable.HashSet[UUID]()
        val studyProgramLanguages = mutable.HashSet[String]()
        val studyProgramSeasons = mutable.HashSet[String]()
        val studyProgramLocations = mutable.HashSet[String]()
        val studyProgramDirectors = mutable.HashSet[String]()
        val studyProgramExamDirectors = mutable.HashSet[String]()

        sf.foreach { case (((((_, f), lang), s), loc), p) =>
          f.foreach(studyForms += _.id)
          lang.foreach(studyProgramLanguages += _.language)
          s.foreach(studyProgramSeasons += _.season)
          loc.foreach(studyProgramLocations += _.location)
          p.foreach { p =>
            p.role match {
              case UniversityRole.SGL => studyProgramDirectors += p.person
              case UniversityRole.PAV => studyProgramExamDirectors += p.person
            }
          }
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
          studyProgramDirectors.toList,
          studyProgramExamDirectors.toList,
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
    val studyProgramPeople = ListBuffer[StudyProgramPersonDbEntry]()

    ls.foreach { sp =>
      studyPrograms += toDbEntry(sp)
      studyProgramLanguages ++= toLanguages(sp)
      studyProgramSeasons ++= toSeasons(sp)
      studyProgramLocations ++= toLocation(sp)
      studyProgramPeople ++= toPeople(sp)
      val (sfs, sfscs) = toStudyForms(sp)
      studyForms ++= sfs
      studyFormScopes ++= sfscs
    }

    val action = for {
      _ <- tableQuery ++= studyPrograms
      _ <- studyFormTableQuery ++= studyForms
      _ <- studyFormScopeTableQuery ++= studyFormScopes
      _ <- studyProgramLanguageTableQuery ++= studyProgramLanguages
      _ <- studyProgramSeasonTableQuery ++= studyProgramSeasons
      _ <- studyProgramLocationTableQuery ++= studyProgramLocations
      _ <- studyProgramPersonTableQuery ++= studyProgramPeople
    } yield ()

    db.run(action.transactionally)
  }

  def create(sp: StudyProgram) = {
    val action = for {
      _ <- tableQuery += toDbEntry(sp)
      _ <- createDependencies(sp)
    } yield sp

    db.run(action.transactionally)
  }

  def update(sp: StudyProgram) =
    db.run(updateAction(sp))

  def exists(sp: StudyProgram): Future[Boolean] =
    db.run(tableQuery.filter(_.abbrev === sp.abbrev).exists.result)

  private def toLanguages(sp: StudyProgram) =
    sp.language.map(l => StudyProgramLanguageDbEntry(l.abbrev, sp.abbrev))

  private def toSeasons(sp: StudyProgram) =
    sp.seasons.map(s => StudyProgramSeasonDbEntry(s.abbrev, sp.abbrev))

  private def toLocation(sp: StudyProgram) =
    sp.campus.map(c => StudyProgramLocationDbEntry(c.abbrev, sp.abbrev))

  private def toPeople(sp: StudyProgram) =
    sp.programDirectors.map(p =>
      StudyProgramPersonDbEntry(p.id, sp.abbrev, UniversityRole.SGL)
    ) :::
      sp.examDirectors.map(p =>
        StudyProgramPersonDbEntry(p.id, sp.abbrev, UniversityRole.PAV)
      )

  private def toStudyForms(sp: StudyProgram) = {
    val studyForms = ListBuffer[StudyFormDbEntry]()
    val studyFormScopes = ListBuffer[StudyFormScopeDbEntry]()
    sp.studyForm.foreach { sf =>
      val sfId = UUID.randomUUID()
      studyForms += StudyFormDbEntry(
        sfId,
        sp.abbrev,
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
    (studyForms.toList, studyFormScopes.toList)
  }

  private def createDependencies(sp: StudyProgram) = {
    val (sfs, sfscs) = toStudyForms(sp)
    for {
      _ <- studyFormTableQuery ++= sfs
      _ <- studyFormScopeTableQuery ++= sfscs
      _ <- studyProgramLanguageTableQuery ++= toLanguages(sp)
      _ <- studyProgramSeasonTableQuery ++= toSeasons(sp)
      _ <- studyProgramLocationTableQuery ++= toLocation(sp)
      _ <- studyProgramPersonTableQuery ++= toPeople(sp)
    } yield sp
  }

  private def updateAction(sp: StudyProgram) =
    for {
      _ <- studyProgramLocationTableQuery
        .filter(_.studyProgram === sp.abbrev)
        .delete
      _ <- studyProgramSeasonTableQuery
        .filter(_.studyProgram === sp.abbrev)
        .delete
      _ <- studyProgramLanguageTableQuery
        .filter(_.studyProgram === sp.abbrev)
        .delete
      _ <- studyFormScopeTableQuery
        .filter(s =>
          s.studyForm in studyFormTableQuery
            .filter(_.studyProgram === sp.abbrev)
            .map(_.id)
        )
        .delete
      _ <- studyFormTableQuery.filter(_.studyProgram === sp.abbrev).delete
      _ <- studyProgramPersonTableQuery
        .filter(_.studyProgram === sp.abbrev)
        .delete
      _ <- tableQuery.filter(_.abbrev === sp.abbrev).update(toDbEntry(sp))
      _ <- createDependencies(sp)
    } yield sp

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
      sp.accreditationUntil,
      sp.restrictedAdmission,
      sp.deDescription,
      sp.deNote,
      sp.enDescription,
      sp.enNote
    )
}
