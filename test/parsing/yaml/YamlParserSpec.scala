package parsing.yaml

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.{
  ParserSpecHelper,
  multilineStringForKey,
  singleLineStringForKey,
  stringForKey
}

final class YamlParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Yaml Parser Spec" should {
    "parse a single string value" in {
      val input1 = "foo: bar"
      val (res1, rest1) = singleLineStringForKey("foo").parse(input1)
      assert(rest1.isEmpty)
      assert(res1.value == "bar")

      val input2 = "foo: bar\n"
      val (res2, rest2) = singleLineStringForKey("foo").parse(input2)
      assert(rest2.isEmpty)
      assert(res2.value == "bar")
    }

    "parse a multiline string value" in {
      val input1 =
        """foo: >
          |paragraph1
          |paragraph2
          |paragraph3""".stripMargin
      val (res1, rest1) = multilineStringForKey("foo").parse(input1)
      assert(rest1.isEmpty)
      assert(res1.value == "paragraph1 paragraph2 paragraph3\n")

      val input2 =
        """foo: |
          |paragraph1
          |paragraph2
          |paragraph3""".stripMargin
      val (res2, rest2) = multilineStringForKey("foo").parse(input2)
      assert(rest2.isEmpty)
      assert(res2.value == "paragraph1\nparagraph2\nparagraph3\n")
    }

    "parse a string value" in {
      val input1 =
        """foo: >
          |paragraph1
          |paragraph2
          |paragraph3""".stripMargin
      val (res1, rest1) = stringForKey("foo").parse(input1)
      assert(rest1.isEmpty)
      assert(res1.value == "paragraph1 paragraph2 paragraph3\n")

      val input2 = "foo: bar"
      val (res2, rest2) = stringForKey("foo").parse(input2)
      assert(rest2.isEmpty)
      assert(res2.value == "bar")
    }
  }
}
