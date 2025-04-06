package parsing.core

import io.circe.Decoder
import io.circe.HCursor
import models.core.Specialization
import monocle.macros.GenLens
import parsing.validator.POValidator

object SpecializationFileParser extends YamlFileParser[Specialization] {
  def fileParser(implicit pos: Seq[String]) =
    super.fileParser(
      new POValidator[Specialization](pos, GenLens[Specialization](_.po))
    )

  protected override def decoder: Decoder[Specialization] =
    (c: HCursor) => {
      val key = c.key.get
      val obj = c.root.downField(key)
      for {
        label        <- obj.get[String]("label")
        abbreviation <- obj.get[String]("abbreviation")
        po           <- obj.get[String]("po")
      } yield Specialization(key, label, abbreviation, po)
    }
}
