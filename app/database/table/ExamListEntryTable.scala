package database.table

import java.util.UUID

import database.Schema
import service.artifact.examlist.ExamListEntry
import play.api.libs.json.JsValue
import slick.jdbc.PostgresProfile.api.*

private[database] final class ExamListEntryTable(tag: Tag)
    extends Table[ExamListEntry](tag, Some(Schema.Exams.name), "exam_list_entry") {

  import database.MyPostgresProfile.MyAPI.playJsonTypeMapper
  import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper

  def id = column[UUID]("id", O.PrimaryKey)

  def examList = column[UUID]("exam_list")

  def module = column[UUID]("module")

  def moduleTitle = column[String]("module_title")

  def moduleAbbrev = column[String]("module_abbrev")

  def mandatory = column[Boolean]("mandatory")

  def po = column[JsValue]("po")

  def assessmentMethods = column[List[String]]("assessment_methods")

  def firstExaminer = column[String]("first_examiner")

  def secondExaminer = column[String]("second_examiner")

  def examPhases = column[List[String]]("exam_phases")

  def exams = column[JsValue]("exams")

  override def * =
    (
      id,
      examList,
      module,
      moduleTitle,
      moduleAbbrev,
      mandatory,
      po,
      assessmentMethods,
      firstExaminer,
      secondExaminer,
      examPhases,
      exams
    ) <> (ExamListEntry.apply, ExamListEntry.unapply)
}
