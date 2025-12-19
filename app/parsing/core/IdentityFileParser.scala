package parsing.core

import io.circe.Decoder
import io.circe.HCursor
import models.core.Identity
import models.EmploymentType
import parser.Parser
import parser.Parser.*
import parsing.getList
import parsing.validator.FacultyValidator

object IdentityFileParser extends YamlFileParser[Identity] {

  def parser(): Parser[List[Identity]] =
    super.fileParser(new FacultyValidator())

  protected override def decoder: Decoder[Identity] =
    (c: HCursor) => {
      val key = c.key.get
      val obj = c.root.downField(key)

      obj
        .get[String]("label")
        .map(l => if key == "nn" then Identity.Unknown(key, l) else Identity.Group(key, l))
        .orElse(
          for {
            lastname       <- obj.get[String]("lastname")
            firstname      <- obj.get[String]("firstname")
            title          <- obj.getOrElse[String]("title")("")
            faculties      <- obj.getList("faculty")
            abbreviation   <- obj.get[String]("abbreviation")
            campusId       <- obj.get[Option[String]]("campusid")
            isActive       <- obj.get[String]("status").map(_ == "active")
            employmentType <- obj.get[Option[String]]("employment_type")
            websiteUrl     <- obj.get[Option[String]]("website_url")
          } yield Identity
            .Person(
              key,
              lastname.trim,
              firstname.trim,
              title.trim,
              faculties,
              abbreviation.trim,
              campusId.map(_.trim),
              isActive,
              employmentType.fold(EmploymentType.Unknown)(EmploymentType.apply),
              websiteUrl
            )
        )
    }
}
