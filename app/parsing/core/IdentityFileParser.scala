package parsing.core

import io.circe.Decoder
import io.circe.HCursor
import models.core.Identity
import models.core.PersonStatus
import parser.Parser
import parser.Parser.*
import parsing.validator.FacultyValidator
import parsing.CursorOps

object IdentityFileParser extends YamlFileParser[Identity] {

  def fileParser(faculties: Seq[String]): Parser[List[Identity]] =
    super.fileParser(new FacultyValidator(faculties))

  protected override def decoder: Decoder[Identity] =
    (c: HCursor) => {
      val key = c.key.get
      val obj = c.root.downField(key)

      obj
        .get[String]("label")
        .map(l => if key == "nn" then Identity.Unknown(key, l) else Identity.Group(key, l))
        .orElse(
          for {
            lastname     <- obj.get[String]("lastname")
            firstname    <- obj.get[String]("firstname")
            title        <- obj.getOrElse[String]("title")("")
            faculties    <- obj.getList("faculty")
            abbreviation <- obj.get[String]("abbreviation")
            campusId     <- obj.getOrElse[String]("campusid")("")
            status       <- obj.get[String]("status").map(PersonStatus.apply)
          } yield Identity
            .Person(key, lastname.trim, firstname.trim, title.trim, faculties, abbreviation.trim, campusId.trim, status)
        )
    }
}
