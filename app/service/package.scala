import parsing.types.{Content, ParsedMetadata}

import scala.concurrent.{ExecutionContext, Future}

package object service {
  type Result[A] = Future[Either[Seq[PipelineError], Seq[A]]]

  type ParsingResult = Result[(Print, ParsedMetadata, Content, Content)]

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
