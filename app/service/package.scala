import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import parsing.types.ModuleContent
import parsing.types.ParsedMetadata

package object service {
  type Result[A] = Future[Either[Seq[PipelineError], Seq[A]]]

  type ParsingResult = Result[(Print, ParsedMetadata, ModuleContent, ModuleContent)]

  def continueWith[A, B](e: Either[PipelineError, A])(
      f: A => Future[Either[PipelineError, B]]
  )(implicit ctx: ExecutionContext): Future[Either[PipelineError, (A, B)]] =
    e match {
      case Left(errs) =>
        Future.successful(Left(errs))
      case Right(a) =>
        f(a).map(_.map(b => a -> b))
    }
}
