import scala.collection.mutable.ListBuffer

package object validator {
  type Validation[A] = Either[List[String], A]

  case class Validator[A, B](validate: A => Validation[B]) {
    def zip[C](that: Validator[A, C]): Validator[A, (B, C)] =
      Validator { a =>
        var maybeB = Option.empty[B]
        var maybeC = Option.empty[C]
        val errs   = ListBuffer[String]()
        this.validate(a) match {
          case Right(b)  => maybeB = Some(b)
          case Left(err) => errs ++= err
        }
        that.validate(a) match {
          case Right(c)  => maybeC = Some(c)
          case Left(err) => errs ++= err
        }
        if (maybeC.isDefined && maybeB.isDefined)
          Right((maybeB.get, maybeC.get))
        else
          Left(errs.toList)
      }

    def map[C](f: (A, B) => C): Validator[A, C] =
      Validator(a => this.validate(a).map(f(a, _)))

    def flatMap[C](f: (A, B) => Validator[A, C]): Validator[A, C] =
      Validator(a => this.validate(a).flatMap(b => f(a, b).validate(a)))

    def pullback[C](toLocalValue: C => A): Validator[C, B] =
      Validator { globalValue =>
        this.validate(toLocalValue(globalValue))
      }
  }

  case class SimpleValidator[A](validate: A => Validation[A]) {
    def pullback[B](toLocalValue: B => A): Validator[B, A] =
      Validator { globalValue =>
        this.validate(toLocalValue(globalValue))
      }
  }

}
