package parsing.core

import io.circe.Decoder
import io.circe.HCursor
import models.core.FocusArea
import monocle.macros.GenLens
import parser.Parser
import parsing.validator.StudyProgramValidator

object FocusAreaFileParser extends YamlFileParser[FocusArea] {

  def fileParser(implicit programs: Seq[String]): Parser[List[FocusArea]] =
    super.fileParser(
      new StudyProgramValidator[FocusArea](
        programs,
        GenLens[FocusArea](_.program)
      )
    )

  override def decoder: Decoder[FocusArea] =
    (c: HCursor) => {
      val key = c.key.get
      val obj = c.root.downField(key)
      for {
        program <- obj.get[String]("program")
        deLabel <- obj.get[String]("de_label")
        enLabel <- obj.getOrElse[String]("en_label")("")
        deDesc  <- obj.getOrElse[String]("de_desc")("")
        enDesc  <- obj.getOrElse[String]("en_desc")("")
      } yield FocusArea(key, program, deLabel, enLabel, deDesc, enDesc)
    }
}
