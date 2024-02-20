package providers

import parsing.metadata._

import javax.inject.{Inject, Provider, Singleton}

@Singleton()
final class MetadataParserProvider @Inject() (
) extends Provider[Set[MetadataParser]] {
  override def get() = Set(
    new THKV1Parser()
  )
}
