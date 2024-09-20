package parsing.core

import java.time.LocalDate

import io.circe.Decoder
import io.circe.HCursor
import models.core.PO
import monocle.macros.GenLens
import parsing._
import parsing.validator.StudyProgramValidator

object POFileParser extends YamlFileParser[PO] {

  def fileParser(implicit programs: Seq[String]) = super.fileParser(
    new StudyProgramValidator[PO](
      programs,
      GenLens[PO](_.program)
    )
  )

  override def decoder: Decoder[PO] =
    (c: HCursor) => {
      val key = c.key.get
      val obj = c.root.downField(key)
      for {
        version  <- obj.get[Int]("version")
        dateFrom <- obj.get[LocalDate]("date_from")
        dateTo   <- obj.get[Option[LocalDate]]("date_to")
        program  <- obj.get[String]("program")
      } yield PO(key, version, program, dateFrom, dateTo)
    }
}
