package parsing.validator

trait YamlFileParserValidator[A] {
  def expected(): String
  def validate(a: A): Either[String, A]
}
