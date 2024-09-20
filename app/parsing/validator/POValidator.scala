package parsing.validator

import monocle.Lens

final class POValidator[A](
    pos: Seq[String],
    po: Lens[A, String]
) extends YamlFileParserValidator[A] {
  override def expected(): String = pos.mkString(", ")

  override def validate(a: A): Either[String, A] = {
    val po = this.po.get(a).stripPrefix("po.")
    Either.cond(
      pos.contains(po),
      this.po.replace(po).apply(a),
      this.po.get(a)
    )
  }
}
