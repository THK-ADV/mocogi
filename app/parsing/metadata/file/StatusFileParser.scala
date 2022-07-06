package parsing.metadata.file

import parsing.helper.SimpleFileParser2
import parsing.types.Status

import javax.inject.Singleton

@Singleton
final class StatusFileParser extends SimpleFileParser2[Status] {
  override protected def makeType = Status.tupled
}
