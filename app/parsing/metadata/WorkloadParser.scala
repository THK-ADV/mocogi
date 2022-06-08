package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps.{P2, P3, P4, P5}
import parsing.intForKey
import parsing.types.Workload

object WorkloadParser {
  val workloadParser: Parser[Workload] = {
    def isValid(w: Workload) = {
      val sum = w.lecture + w.seminar + w.practical + w.exercise + w.selfStudy
      Option.when(sum != w.total)(sum)
    }

    intForKey("workload")
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .zip(intForKey("lecture"))
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .take(intForKey("seminar"))
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .take(intForKey("practical"))
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .take(intForKey("exercise"))
      .skip(newline)
      .skip(zeroOrMoreSpaces)
      .take(intForKey("self_study"))
      .skip(newline)
      .map(Workload.tupled)
      .flatMap(w =>
        isValid(w).fold(always(w))(i =>
          never(s"total of workload to be ${w.total}, but was $i")
        )
      )
  }
}
