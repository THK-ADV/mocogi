package service.core

import parser.Parser
import parsing.core.FileParser

import scala.concurrent.Future

trait SimpleYamlService[A] extends YamlService[A, A] {
  def fileParser: FileParser[A]

  override def toInput(output: A): A = output

  override def parser: Future[Parser[List[A]]] =
    Future.successful(fileParser.fileParser)
}
