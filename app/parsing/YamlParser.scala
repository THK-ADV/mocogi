package parsing

import git.GitFileContent

object YamlParser {

  /**
   * Validates the given yaml file to the yaml spec. The implementation is not
   * efficient, since it converts the entire file to json.
   * @param yaml the yaml file to check
   * @return parsing error or none
   */
  def validateYaml(yaml: String): Option[String] = {
    io.circe.yaml.parser.parse(yaml) match
      case Left(value) => Some(value.message)
      case Right(_)    => None
  }

  /**
   * Validates the given module content to the yaml spec.
   * @param gitFileContent the module file content
   * @return parsing error or none
   */
  def validateModuleYaml(gitFileContent: GitFileContent): Option[String] = {
    val first = gitFileContent.value.dropWhile(_ != '\n').drop(1)
    val yaml  = first.slice(0, first.indexOf("---"))
    validateYaml(yaml)
  }
}
