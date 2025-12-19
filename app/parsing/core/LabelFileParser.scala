package parsing.core

import io.circe.Decoder
import io.circe.HCursor
import models.core.IDLabel

private[core] trait LabelFileParser[A <: IDLabel] extends YamlFileParser[A] {

  protected def makeType: ((String, String, String)) => A

  override def decoder: Decoder[A] =
    (c: HCursor) => {
      val key = c.key.get
      val obj = c.root.downField(key)
      for {
        deLabel <- obj.get[String]("de_label")
        enLabel <- obj.get[String]("en_label")
      } yield makeType(key, deLabel.trim, enLabel.trim)
    }
}
