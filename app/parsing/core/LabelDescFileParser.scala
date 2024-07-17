package parsing.core

import io.circe.{Decoder, HCursor}
import models.core.IDLabelDesc

trait LabelDescFileParser[A <: IDLabelDesc] extends YamlFileParser[A] {

  protected def makeType: ((String, String, String, String, String)) => A

  override def decoder: Decoder[A] =
    (c: HCursor) => {
      val key = c.key.get
      val obj = c.root.downField(key)
      for {
        deLabel <- obj.get[String]("de_label")
        enLabel <- obj.get[String]("en_label")
        deDesc <- obj.getOrElse[String]("de_desc")("")
        enDesc <- obj.getOrElse[String]("en_desc")("")
      } yield makeType(key, deLabel.trim, enLabel.trim, deDesc.trim, enDesc.trim)
    }
}
