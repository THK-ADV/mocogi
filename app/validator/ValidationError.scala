package validator

import java.util.UUID

case class ValidationError(id: UUID, title: String, errs: List[String])
    extends Throwable {

  override def getLocalizedMessage = getMessage

  override def getMessage =
    s"""
       |Validation Error:
       |
       |Module: $title
       |
       |Errors:
       |${errs.mkString("\n")}""".stripMargin
}
