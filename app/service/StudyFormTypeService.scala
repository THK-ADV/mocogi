package service

import basedata.StudyFormType
import parsing.base.{FileParser, StudyFormTypeFileParser}

object StudyFormTypeService extends YamlService[StudyFormType] {
  override def repo = ???

  override def parser: FileParser[StudyFormType] = StudyFormTypeFileParser
}
