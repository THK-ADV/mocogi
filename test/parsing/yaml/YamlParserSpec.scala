package parsing.yaml

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.{
  >,
  ParserSpecHelper,
  Plain,
  multilineStringForKey,
  multilineStringStrategy,
  removeIndentation,
  shiftSpaces,
  singleLineStringForKey,
  stringForKey,
  |
}

final class YamlParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Yaml Parser Spec" when {
    "parse a multiline string strategy" should {
      "return >" in {
        val (res1, rest1) = multilineStringStrategy.parse(">")
        assert(res1.value == >)
        assert(rest1.isEmpty)

        val (res2, rest2) = multilineStringStrategy.parse(">\nabc")
        assert(res2.value == >)
        assert(rest2 == "\nabc")
      }

      "return |" in {
        val (res1, rest1) = multilineStringStrategy.parse("|")
        assert(rest1.isEmpty)
        assert(res1.value == |)

        val (res2, rest2) = multilineStringStrategy.parse("|\nabc")
        assert(rest2 == "\nabc")
        assert(res2.value == |)
      }

      "return pure string" in {
        val (res1, rest1) = multilineStringStrategy.parse("\n")
        assert(rest1 == "\n")
        assert(res1.value == Plain)

        val (res2, rest2) = multilineStringStrategy.parse(" ")
        assert(rest2.isEmpty)
        assert(res2.value == Plain)

        val (res3, rest3) = multilineStringStrategy.parse(" \nabc")
        assert(rest3 == "\nabc")
        assert(res3.value == Plain)
      }

      "fail if the strategy in unknown" in {
        assertError(
          multilineStringStrategy,
          "abc",
          "'>' or '|' or space or newline",
          Some("")
        )
      }
    }

    "parse a single string value" in {
      val input1 = "foo: bar"
      val (res1, rest1) = singleLineStringForKey("foo").parse(input1)
      assert(rest1.isEmpty)
      assert(res1.value == "bar")

      val input2 = "foo: bar\n"
      val (res2, rest2) = singleLineStringForKey("foo").parse(input2)
      assert(rest2.isEmpty)
      assert(res2.value == "bar")

      val input3 = "foo: bar\nbaz: bar"
      val (res3, rest3) = singleLineStringForKey("foo").parse(input3)
      assert(rest3 == "baz: bar")
      assert(res3.value == "bar")
    }

    "remove indentations" in {
      val input0 =
        """foo: okay
          |  bar:
          |    paragraph1
          |    paragraph2
          |    paragraph3
          |  baz: ok
          |boom: ko""".stripMargin
      val output0 =
        """foo: okay
          |bar:
          |  paragraph1
          |  paragraph2
          |  paragraph3
          |baz: ok
          |boom: ko""".stripMargin
      val (res0, rest0) = removeIndentation().parse(input0)
      assert(res0.isRight)
      assert(rest0 == output0)

      val output1 =
        """foo: okay
          |bar:
          |paragraph1
          |paragraph2
          |paragraph3
          |baz: ok
          |boom: ko""".stripMargin
      val (res1, rest1) = removeIndentation(2).parse(input0)
      assert(res1.isRight)
      assert(rest1 == output1)

      val output2 =
        """foo: okay
          |bar:
          |paragraph1
          |paragraph2
          |paragraph3
          |baz: ok
          |boom: ko""".stripMargin
      val (res2, rest2) = removeIndentation(3).parse(input0)
      assert(res2.isRight)
      assert(rest2 == output2)
    }

    "parse a multiline string value" in {
      val input0 =
        """foo:
          |  paragraph1
          |  paragraph2
          |  paragraph3""".stripMargin
      val (res0, rest0) = multilineStringForKey("foo").parse(input0)
      assert(rest0.isEmpty)
      assert(res0.value == "paragraph1 paragraph2 paragraph3")

      val input1 =
        """foo: >
          |  paragraph1
          |  paragraph2
          |  paragraph3""".stripMargin
      val (res1, rest1) = multilineStringForKey("foo").parse(input1)
      assert(res1.value == "paragraph1 paragraph2 paragraph3\n")
      assert(rest1.isEmpty)

      val input2 =
        """foo: |
          |  paragraph1
          |  paragraph2
          |  paragraph3""".stripMargin
      val (res2, rest2) = multilineStringForKey("foo").parse(input2)
      assert(rest2.isEmpty)
      assert(res2.value == "paragraph1\nparagraph2\nparagraph3\n")

      val input3 =
        """foo: |
          |  paragraph1
          |  paragraph2
          |  paragraph3
          |""".stripMargin
      val (res3, rest3) = multilineStringForKey("foo").parse(input3)
      assert(rest3.isEmpty)
      assert(res3.value == "paragraph1\nparagraph2\nparagraph3\n")

      val input4 =
        """foo: >
          |  paragraph1
          |  paragraph2
          |  paragraph3
          |""".stripMargin
      val (res4, rest4) = multilineStringForKey("foo").parse(input4)
      assert(rest4.isEmpty)
      assert(res4.value == "paragraph1 paragraph2 paragraph3\n")

      val input5 =
        """foo: |
          |  paragraph1
          |  paragraph2
          |  paragraph3
          |bar: baz""".stripMargin
      val (res5, rest5) = multilineStringForKey("foo").parse(input5)
      assert(rest5 == "bar: baz")
      assert(res5.value == "paragraph1\nparagraph2\nparagraph3\n")
    }

    "parse multiline string value with explicit newlines" in {
      val input0 =
        """foo:
          |  paragraph1
          |
          |  paragraph2
          |
          |  paragraph3""".stripMargin
      val (res0, rest0) = multilineStringForKey("foo").parse(input0)
      assert(rest0.isEmpty)
      assert(res0.value == "paragraph1\nparagraph2\nparagraph3")

      val input1 =
        """foo: >
          |  paragraph1
          |
          |  paragraph2
          |
          |  paragraph3""".stripMargin
      val (res1, rest1) = multilineStringForKey("foo").parse(input1)
      assert(rest1.isEmpty)
      assert(res1.value == "paragraph1\nparagraph2\nparagraph3\n")

      val input2 =
        """foo: |
          |  paragraph1
          |
          |  paragraph2
          |  paragraph3""".stripMargin
      val (res2, rest2) = multilineStringForKey("foo").parse(input2)
      assert(rest2.isEmpty)
      assert(res2.value == "paragraph1\n\nparagraph2\nparagraph3\n")
    }

    "parse a string value" in {
      val input1 =
        """foo: >
          |  paragraph1
          |  paragraph2
          |  paragraph3""".stripMargin
      val (res1, rest1) = stringForKey("foo").parse(input1)
      assert(rest1.isEmpty)
      assert(res1.value == "paragraph1 paragraph2 paragraph3\n")

      val input2 = "foo: bar"
      val (res2, rest2) = stringForKey("foo").parse(input2)
      assert(rest2.isEmpty)
      assert(res2.value == "bar")

      val input3 =
        """foo: >
          |  paragraph1
          |  paragraph2
          |  paragraph3
          |""".stripMargin
      val (res3, rest3) = stringForKey("foo").parse(input3)
      assert(rest3.isEmpty)
      assert(res3.value == "paragraph1 paragraph2 paragraph3\n")

      val input4 =
        """foo: |
          |  paragraph1
          |  paragraph2
          |  paragraph3
          |bar: baz""".stripMargin
      val (res4, rest4) = stringForKey("foo").parse(input4)
      assert(rest4 == "bar: baz")
      assert(res4.value == "paragraph1\nparagraph2\nparagraph3\n")
    }

    "test shift spaces" in {
      val input1 = List("paragraph1", " paragraph2")
      val output1 = List("paragraph1\n", "paragraph2")
      assert(shiftSpaces(input1) == output1)

      val input2 = List("paragraph1", "paragraph2")
      val output2 = List("paragraph1", "paragraph2")
      assert(shiftSpaces(input2) == output2)

      val input3 = List("paragraph1", " paragraph2", " paragraph3")
      val output3 = List("paragraph1\n", "paragraph2\n", "paragraph3")
      assert(shiftSpaces(input3) == output3)

      val input4 = List("paragraph1", " paragraph2", "paragraph3")
      val output4 = List("paragraph1\n", "paragraph2", "paragraph3")
      assert(shiftSpaces(input4) == output4)

      val input5 = List("paragraph1", "paragraph2", " paragraph3")
      val output5 = List("paragraph1", "paragraph2\n", "paragraph3")
      assert(shiftSpaces(input5) == output5)
    }
  }
}
