package parsing.core

import cats.syntax.either._
import io.circe.Decoder
import io.circe.yaml.parser.parse
import parser.{Parser, ParsingError}
import parsing.validator.YamlFileParserValidator

trait YamlFileParser[A] {
  protected def decoder: Decoder[A]

  protected def fileParser(
      validator: YamlFileParserValidator[A]
  ): Parser[List[A]] =
    Parser { str =>
      val res = parse(str)
        .flatMap(_.as(parsing.decoderList(decoder)))
        .leftMap(e => ParsingError(e.getMessage, str))
        .flatMap(validate(_, validator))
      val rest = if (res.isRight) "" else str
      (res, rest)
    }

  protected def fileParser(): Parser[List[A]] =
    Parser { str =>
      val res = parse(str)
        .flatMap(_.as(parsing.decoderList(decoder)))
        .leftMap(e => ParsingError(e.getMessage, str))
      val rest = if (res.isRight) "" else str
      (res, rest)
    }

  private def validate(
      xs: List[A],
      validator: YamlFileParserValidator[A]
  ): Either[ParsingError, List[A]] = {
    val (invalid, valid) = xs.partitionMap(validator.validate)
    Either.cond(
      invalid.isEmpty,
      valid,
      ParsingError(
        validator.expected(),
        invalid.mkString(", ")
      )
    )
  }
}
