package parsing.validator

import cats.data.NonEmptyList

final class CombineValidators[A](
    validators: NonEmptyList[YamlFileParserValidator[A]]
) extends YamlFileParserValidator[A] {

  override def expected(): String =
    validators.map(_.expected()).toList.mkString(" or ")

  override def validate(a: A): Either[String, A] =
    validators.foldLeft[Either[String, A]](Right(a))((acc, validator) => acc.flatMap(validator.validate))
}
