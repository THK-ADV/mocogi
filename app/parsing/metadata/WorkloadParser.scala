package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps.{P0, P2, P3, P4, P5}
import parsing.posIntForKey
import parsing.types.Workload

object WorkloadParser {

  val workloadParser: Parser[Workload] =
    prefix("workload:")
      .skip(zeroOrMoreSpaces)
      .take(posIntForKey("lecture"))
      .skip(zeroOrMoreSpaces)
      .zip(posIntForKey("seminar"))
      .skip(zeroOrMoreSpaces)
      .take(posIntForKey("practical"))
      .skip(zeroOrMoreSpaces)
      .take(posIntForKey("exercise"))
      .skip(zeroOrMoreSpaces)
      .take(posIntForKey("project_supervision"))
      .skip(zeroOrMoreSpaces)
      .take(posIntForKey("project_work"))
      .map(a => Workload(a._1, a._2, a._3, a._4, a._5, a._6))
}
