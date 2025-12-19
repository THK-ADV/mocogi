package parsing.validator

import cats.data.NonEmptyList
import monocle.Lens

private[parsing] final class IdentitiesValidator[A](
    identities: Seq[String],
    identity: Lens[A, NonEmptyList[String]]
) extends YamlFileParserValidator[A] {
  override def expected(): String = identities.mkString(", ")

  override def validate(a: A): Either[String, A] = {
    val identity = this.identity.get(a).map(_.stripPrefix("person."))
    Either.cond(
      identity.forall(identities.contains),
      this.identity.replace(identity).apply(a),
      this.identity.get(a).toList.mkString(", ")
    )
  }
}
