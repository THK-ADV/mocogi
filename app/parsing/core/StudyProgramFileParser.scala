package parsing.core

import cats.data.NonEmptyList
import io.circe.Decoder
import io.circe.HCursor
import models.core._
import monocle.macros.GenLens
import parser.Parser
import parsing.validator.CombineValidators
import parsing.validator.DegreeValidator
import parsing.validator.IdentitiesValidator
import parsing.CursorOps

object StudyProgramFileParser extends YamlFileParser[StudyProgram] {

  def fileParser(implicit degrees: Seq[Degree], identities: Seq[Identity]): Parser[List[StudyProgram]] = {
    val identityIds = identities.map(_.id)
    super.fileParser(
      new CombineValidators(
        NonEmptyList.of(
          new DegreeValidator(
            degrees.map(_.id),
            GenLens[StudyProgram](_.degree)
          ),
          new IdentitiesValidator(
            identityIds,
            GenLens[StudyProgram](_.programDirectors)
          ),
          new IdentitiesValidator(
            identityIds,
            GenLens[StudyProgram](_.examDirectors)
          )
        )
      )
    )
  }

  protected override def decoder: Decoder[StudyProgram] =
    (c: HCursor) => {
      val key = c.key.get
      val obj = c.root.downField(key)
      for {
        deLabel         <- obj.get[String]("de_label")
        enLabel         <- obj.get[String]("en_label")
        degree          <- obj.get[String]("grade")
        programDirector <- obj.getNonEmptyList("program_director")
        examDirector    <- obj.getNonEmptyList("exam_director")
      } yield StudyProgram(
        key,
        deLabel,
        enLabel,
        degree,
        programDirector,
        examDirector
      )
    }
}
