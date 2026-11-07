package database.table

import java.util.UUID

import database.Schema
import slick.jdbc.PostgresProfile.api.*

private[database] final class ExamTable(tag: Tag) extends Table[(UUID, String)](tag, Some(Schema.Exams.name), "exam") {

  def id = column[UUID]("id", O.PrimaryKey)

  def name = column[String]("name")

  override def * = (id, name)
}

/** Assignment of an exam to a module with own exams. */
private[database] final class ExamModuleTable(tag: Tag)
    extends Table[(UUID, UUID)](tag, Some(Schema.Exams.name), "exam_module") {

  def exam = column[UUID]("exam")

  def module = column[UUID]("module")

  override def * = (exam, module)
}
