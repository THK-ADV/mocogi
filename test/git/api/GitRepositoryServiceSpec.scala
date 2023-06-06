package git.api

import git.api.GitRepositoryService.{linkParser, nextLinkParser}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}

final class GitRepositoryServiceSpec
    extends AnyWordSpec
    with EitherValues
    with OptionValues {

  def input1 =
    "<https://foo.bar.baz/id=42&page=2&pagination=legacy&path=modules&per_page=20&recursive=false>; rel=\"next\", <https://foo.bar.baz/id=42&page=1&pagination=legacy&path=modules&per_page=20&recursive=false>; rel=\"first\", <https://foo.bar.baz/id=42&page=17&pagination=legacy&path=modules&per_page=20&recursive=false>; rel=\"last\""

  def input2 =
    "<https://foo.bar.baz/id=42&page=3&pagination=legacy&path=modules&per_page=100&recursive=false>; rel=\"prev\", <https://foo.bar.baz/id=42&page=1&pagination=legacy&path=modules&per_page=100&recursive=false>; rel=\"first\", <https://foo.bar.baz/id=42&page=4&pagination=legacy&path=modules&per_page=100&recursive=false>; rel=\"last\""

  "A GitRepositoryService" should {
    "parse links for pagination" in {
      val (res1, rest1) = linkParser.parse(input1)
      assert(
        res1.value == List(
          (
            "https://foo.bar.baz/id=42&page=2&pagination=legacy&path=modules&per_page=20&recursive=false",
            "next"
          ),
          (
            "https://foo.bar.baz/id=42&page=1&pagination=legacy&path=modules&per_page=20&recursive=false",
            "first"
          ),
          (
            "https://foo.bar.baz/id=42&page=17&pagination=legacy&path=modules&per_page=20&recursive=false",
            "last"
          )
        )
      )
      assert(rest1.isEmpty)

      val (res2, rest2) = linkParser.parse(input2)
      assert(
        res2.value == List(
          (
            "https://foo.bar.baz/id=42&page=3&pagination=legacy&path=modules&per_page=100&recursive=false",
            "prev"
          ),
          (
            "https://foo.bar.baz/id=42&page=1&pagination=legacy&path=modules&per_page=100&recursive=false",
            "first"
          ),
          (
            "https://foo.bar.baz/id=42&page=4&pagination=legacy&path=modules&per_page=100&recursive=false",
            "last"
          )
        )
      )
      assert(rest2.isEmpty)
    }

    "parse 'next' link for pagination" in {
      val (res1, rest1) = nextLinkParser.parse(input1)
      assert(
        res1.value.value == "https://foo.bar.baz/id=42&page=2&pagination=legacy&path=modules&per_page=20&recursive=false"
      )
      assert(rest1.isEmpty)

      val (res2, rest2) = nextLinkParser.parse(input2)
      assert(res2.value.isEmpty)
      assert(rest2.isEmpty)
    }
  }

}
