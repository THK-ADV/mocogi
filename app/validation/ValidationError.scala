package validation

case class ValidationError(errs: List[String]) extends Throwable {

  override def getLocalizedMessage = getMessage

  override def getMessage =
    s"""
       |Validation Error:
       |
       |Errors:
       |${errs.mkString("\n")}""".stripMargin
}
