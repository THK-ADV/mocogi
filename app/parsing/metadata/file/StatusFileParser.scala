package parsing.metadata.file

import parsing.types.Status

import javax.inject.Singleton

@Singleton
final class StatusFileParser extends LabelFileParser[Status] {
  override protected def makeType = Status.tupled
}
