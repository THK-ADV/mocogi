package parsing.metadata.file

import parsing.helper.SimpleFileParser
import parsing.types.Status

import javax.inject.Singleton

@Singleton
final class StatusFileParser extends SimpleFileParser[Status] {
  override protected def makeType = Status.tupled
}
