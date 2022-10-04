package service

import basedata.PO
import parsing.base.FileParser

object POService extends YamlService[PO] {
  override def repo = ???
  override def parser: FileParser[PO] = ???
}
