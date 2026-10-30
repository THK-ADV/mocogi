package database.repo

import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.core.AssessmentMethodTable
import database.table.core.IdentityTable
import database.table.core.POTable
import database.table.core.SpecializationTable
import database.table.ExamListConfigTable
import database.table.ExamListEntryTable
import database.table.ExamListTable
import database.table.ModuleTable
import database.view.StudyProgramViewRepository
import models.*
import models.core.AssessmentMethod
import models.core.Identity
import service.artifact.examlist.*
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import slick.jdbc.JdbcProfile
import slick.jdbc.TransactionIsolation

@Singleton
final class ExamListRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    val studyProgramViewRepository: StudyProgramViewRepository,
    private implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import database.MyPostgresProfile.MyAPI.getJson
  import database.MyPostgresProfile.MyAPI.playJsonTypeMapper
  import database.MyPostgresProfile.MyAPI.setJson
  import database.MyPostgresProfile.MyAPI.setLocalDate
  import profile.api.*

  private val tableQuery = TableQuery[ExamListTable]

  private val entryQuery = TableQuery[ExamListEntryTable]

  private def studyProgram(po: String) =
    studyProgramViewRepository.tableQuery.filter(s => s.poId === po && s.specializationId.isEmpty)

  /** Sort key of a semester id such as `sose_2027`: the year followed by the abbreviation, so `sose` precedes `wise`. */
  private def chronological(semester: Rep[String]) = semester.drop(5) ++ semester.take(4)

  /** Returns the lists, by default only the latest semester of each PO unless a semester is given. */
  def all(po: Option[String], semester: Option[String], latestOnly: Boolean): Future[Seq[ExamList]] = {
    val query = tableQuery
      .filterOpt(po)(_.po === _)
      .filterOpt(semester)(_.semester === _)
      .filterIf(latestOnly && semester.isEmpty)(l =>
        !tableQuery.filter(o => o.po === l.po && chronological(o.semester) > chronological(l.semester)).exists
      )
      .join(studyProgramViewRepository.tableQuery.filter(_.specializationId.isEmpty))
      .on(_.po === _.poId)
      .result
      .map(_.map((l, sp) => ExamList(l.id, sp, Semester(l.semester), l.date, l.url)))
    db.run(query)
  }

  def semesters(): Future[Seq[String]] = db.run(tableQuery.map(_.semester).distinct.result)

  /** Returns the entries of the list sorted by module title, or `None` if the list does not exist. */
  def entries(id: UUID): Future[Option[Seq[ExamListEntry]]] =
    db.run(for {
      exists  <- tableQuery.filter(_.id === id).exists.result
      entries <- entryQuery.filter(_.examList === id).sortBy(_.moduleTitle).result
    } yield Option.when(exists)(entries))

  /** Returns the stored configuration, or `None` if the PO does not exist. */
  def config(po: String): Future[Option[Option[JsValue]]] =
    db.run(
      TableQuery[POTable]
        .filter(_.id === po)
        .joinLeft(TableQuery[ExamListConfigTable])
        .on(_.id === _.po)
        .map(_._2.map(_.config))
        .result
        .headOption
    )

  /** Inserts or replaces the configuration and returns `false` if the PO does not exist. */
  def saveConfig(po: String, config: ExamListConfig): Future[Boolean] =
    db.run(
      sqlu"""INSERT INTO exams.exam_list_config (po, config)
             SELECT id, ${Json.toJson(config)} FROM core.po WHERE id = $po
             ON CONFLICT (po) DO UPDATE SET config = excluded.config"""
    ).map(_ == 1)

  /**
   * Removes the modules from the configurations of POs they are no longer related to, e.g. after a module sync or
   * deletion. Like for the candidates, the relations of child modules count for their parent module.
   */
  def removeUnrelatedModules(modules: Seq[UUID]): Future[Int] = {
    import database.MyPostgresProfile.MyAPI.setUUIDArray
    db.run(sqlu"""
      UPDATE exams.exam_list_config c
      SET config = config || jsonb_build_object(
        'excludedModuleIds', jsonb_path_query_array(config -> 'excludedModuleIds', '$$[*] ? (!(@ == $$ids[*]))', r.vars),
        'excludedExams', jsonb_path_query_array(config -> 'excludedExams', '$$[*] ? (!(@.module == $$ids[*]))', r.vars),
        'registrationOverrides',
        jsonb_path_query_array(config -> 'registrationOverrides', '$$[*] ? (!(@.module == $$ids[*]))', r.vars))
      FROM (
        SELECT c.po, jsonb_build_object('ids', jsonb_agg(m)) AS vars
        FROM exams.exam_list_config c, unnest(${modules.toList}::uuid[]) m
        WHERE jsonb_path_exists(c.config, '$$.** ? (@ == $$m)', jsonb_build_object('m', m))
          AND m NOT IN (
            SELECT coalesce(r.parent, p.module)
            FROM (SELECT module FROM modules.module_po_mandatory WHERE po = c.po
                  UNION ALL SELECT module FROM modules.module_po_optional WHERE po = c.po) p
            LEFT JOIN modules.module_relation r ON r.child = p.module)
        GROUP BY c.po
      ) r
      WHERE r.po = c.po""")
  }

  /** Returns the candidates of the PO, or `None` if there is no current study program of the PO. */
  def options(po: String): Future[Option[Seq[ExamListModuleOption]]] =
    db.run(
      studyProgram(po).exists.result.flatMap(exists =>
        if exists then optionsAction(po).map(Some(_)) else DBIO.successful(None)
      )
    )

  /** Reads everything needed to prepare the list of the PO consistently, or `None` if there is no study program. */
  def data(po: String): Future[Option[ExamListData]] = {
    val action = studyProgram(po).result.headOption.flatMap {
      case None     => DBIO.successful(None)
      case Some(sp) =>
        for {
          options         <- optionsAction(po)
          specializations <- TableQuery[SpecializationTable].filter(_.po === po).result
          identities      <- TableQuery[IdentityTable]
            .filter(_.id.inSet(options.flatMap(o => List(o.firstExaminer, o.secondExaminer))))
            .result
          methods <- TableQuery[AssessmentMethodTable]
            .filter(_.id.inSet(options.flatMap(_.assessmentMethods.map(_.method))))
            .map(a => (a.id, a.deLabel, a.enLabel))
            .result
          genericModules <- TableQuery[ModuleTable]
            .filter(_.id.inSet(options.flatMap(_.po.flatMap(_.instanceOf))))
            .map(m => (m.id, m.title, m.abbrev))
            .result
        } yield Some(
          ExamListData(
            sp,
            options,
            specializations,
            identities.map(Identity.fromDbEntry),
            methods.map(AssessmentMethod.apply),
            genericModules.map(ModuleCore.apply)
          )
        )
    }
    db.run(action.transactionally.withTransactionIsolation(TransactionIsolation.RepeatableRead))
  }

  /**
   * Replaces the list of the PO and semester by the prepared entries. The list and the entries of retained modules keep
   * their ids. Upserting the list locks its row, which serializes concurrent publications.
   */
  def replace(po: String, semester: String, date: LocalDate, url: String, entries: Seq[PreparedExamListEntry]) = {
    val action = for {
      id <-
        sql"""INSERT INTO exams.exam_list (po, semester, date, url) VALUES ($po, $semester, $date, $url)
                  ON CONFLICT (po, semester) DO UPDATE SET date = excluded.date, url = excluded.url
                  RETURNING id::text""".as[String].head.map(UUID.fromString)
      ids <- entryQuery.filter(_.examList === id).map(e => (e.module, e.id)).result.map(_.toMap)
      _   <- entryQuery.filter(_.examList === id).delete
      _   <- entryQuery ++= entries.map { e =>
        val m = e.module
        ExamListEntry(
          ids.getOrElse(m.id, UUID.randomUUID()),
          id,
          m.id,
          m.title,
          m.abbrev,
          m.mandatory,
          Json.toJson(m.po),
          m.assessmentMethods.map(_.method),
          m.firstExaminer,
          m.secondExaminer,
          e.examPhases,
          Json.toJson(e.exams)
        )
      }
    } yield ()
    db.run(action.transactionally)
  }

  /** Candidates of the PO, see `exams.exam_list_options` in `functions.sql`. */
  private def optionsAction(po: String): DBIO[Seq[ExamListModuleOption]] =
    sql"SELECT exams.exam_list_options($po)".as[JsValue].head.map(_.as[Seq[ExamListModuleOption]])
}
