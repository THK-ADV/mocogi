package parsing.core

import models.core.AssessmentMethod

object AssessmentMethodFileParser extends LabelFileParser[AssessmentMethod] {
  def parser() = this.fileParser()

  override protected def makeType = { case (id, deLabel, enLabel) =>
    AssessmentMethod(id, deLabel, enLabel)
  }
}
