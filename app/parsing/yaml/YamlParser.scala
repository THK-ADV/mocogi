package parsing.yaml

import git.GitFileContent

object YamlParser {

  /**
   * Validates the given YAML content against the YAML specification.
   *
   * NOTE: This implementation is not very efficient, since it first
   * converts the entire YAML document to JSON.
   *
   * @param yaml
   *   The raw YAML text to validate.
   * @return
   *   - Some(errorMessage) if parsing failed,
   *   - None if the YAML is valid.
   */
  def validateYaml(yaml: String): Option[String] = {
    io.circe.yaml.parser.parse(yaml) match
      case Left(value) => Some(value.message)
      case Right(_)    => None
  }

  /**
   * Extracts and validates the YAML “front matter” from a module file.
   *
   * This method assumes that the first line of the file is a header,
   * and that the YAML front matter follows until the next `---` delimiter.
   *
   * @param gitFileContent
   * A container holding the entire module file’s text.
   * @return
   *   - Some(errorMessage) if the extracted YAML is invalid,
   *   - None if parsing succeeds or no front matter is found.
   */
  def validateModuleYaml(gitFileContent: GitFileContent): Option[String] = {
    val first = gitFileContent.value.dropWhile(_ != '\n').drop(1)
    val yaml  = first.slice(0, first.indexOf("---"))
    validateYaml(yaml)
  }
}
