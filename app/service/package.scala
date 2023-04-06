import parsing.types.{Content, ModuleCompendium, ParsedMetadata}

import java.util.UUID
import scala.concurrent.Future

package object service {
  type Result[A] = Future[Either[Seq[PipelineError], Seq[A]]]

  type PrintingResult = Result[(UUID, Print)]
  type ParsingResult = Result[(Print, ParsedMetadata, Content, Content)]
  type ValidationResult = Result[(Print, ModuleCompendium)]

  def continue[A, B](
      e: Either[Seq[PipelineError], Seq[A]],
      f: Seq[A] => Future[Either[Seq[PipelineError], B]]
  ): Future[Either[Seq[PipelineError], B]] =
    e match {
      case Left(errs) =>
        Future.successful(Left(errs))
      case Right(res) =>
        f(res)
    }
}
