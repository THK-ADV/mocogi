package service

trait SimpleYamlService[A] extends YamlService[A, A] {
  override def toInput(output: A) = output
}
