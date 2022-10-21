package service

import basedata.PO
import parsing.base.FileParser

object POService extends YamlService[PO, PO] {
  override def repo = ???
  override def parser: FileParser[PO] = ???

  override def toInput(output: PO) = output
}
