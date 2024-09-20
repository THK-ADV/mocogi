package providers

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import parsing.metadata._

@Singleton()
final class MetadataParserProvider @Inject() (
) extends Provider[Set[MetadataParser]] {
  override def get() = Set(
    new THKV1Parser()
  )
}
