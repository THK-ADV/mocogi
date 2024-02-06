package service.core

import models.ModuleKey
import parsing.modulekeys.ModuleKeyParser
import parsing.withFile0
import service.ModuleProtocolDiff.fields

/** Provides a lookup function for resolving module keys into a localized
  * representation
  */
trait ModuleKeyService {
  def lookup(keys: Set[String]): Set[ModuleKey]
}

object ModuleKeyService {
  def apply(
      parser: ModuleKeyParser,
      moduleKeysPath: String
  ): ModuleKeyService = {
    val keys = parse(parser, moduleKeysPath)
    validate(keys)
    new Impl(keys)
  }

  /** Ensures that each key defined in conf/module_keys.yaml lines up with the
    * the field names defined in ModuleProtocol
    * @param xs
    *   List of module keys to check
    * @throws Throwable
    *   If at least one key does not match
    */
  private def validate(xs: List[ModuleKey]): Unit = {
    val moduleKeys = xs.map(_.id)
    val keys = fields
    val unmatched = moduleKeys.filterNot(keys.contains)
    if (unmatched.nonEmpty)
      throw new Throwable(s"unmatched module keys: ${unmatched.mkString(", ")}")
  }

  /** Parses all module keys defined in path.
    * @param parser
    *   The Parser to use
    * @param path
    *   Location of the file
    * @return
    *   Content of the file
    */
  private def parse(parser: ModuleKeyParser, path: String): List[ModuleKey] =
    withFile0(path)(parser.fileParser.parse)._1
      .fold(throw _, identity)

  private class Impl(moduleKeys: List[ModuleKey]) extends ModuleKeyService {
    def lookup(keys: Set[String]): Set[ModuleKey] =
      keys.map(k =>
        moduleKeys
          .find(_.id == k)
          .getOrElse(throw new Throwable(s"key not found: $k"))
      )
  }
}
