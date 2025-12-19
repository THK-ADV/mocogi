package parsing.metadata

import models.core.Identity
import models.Examiner
import parser.Parser
import parser.Parser.zeroOrMoreSpaces
import parsing.helper.SingleValueParser
import parsing.singleValueRawParser

object ExaminerParser extends SingleValueParser[Identity] {
  def firstKey  = "first_examiner"
  def secondKey = "second_examiner"
  def prefix    = "person."

  private def examinerParser(key: String)(using identities: Seq[Identity]): Parser[Identity] =
    itemParser(
      key,
      identities.sortBy(_.id).reverse,
      x => s"$prefix${x.id}"
    )

  private[parsing] def parser(using Seq[Identity]): Parser[Examiner.Default] =
    examinerParser(firstKey).option
      .skip(zeroOrMoreSpaces)
      .zip(examinerParser(secondKey).option)
      .map(a => Examiner(a._1.getOrElse(Identity.NN), a._2.getOrElse(Identity.NN)))

  private[parsing] def raw: Parser[Examiner.ID] =
    singleValueRawParser(firstKey, prefix).option
      .skip(zeroOrMoreSpaces)
      .zip(singleValueRawParser(secondKey, prefix).option)
      .map(a => Examiner(a._1.getOrElse(Identity.NN.id), a._2.getOrElse(Identity.NN.id)))
}
