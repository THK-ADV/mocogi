package printing

package object latex {
  def escape(str: String) = {
    val buf = new StringBuilder(str.length)
    str.foreach {
      case '_' => buf.append("\\_")
      case '&' => buf.append("\\&")
      case '%' => buf.append("\\%")
      case s   => buf.append(s)
    }
    buf.result()
  }
}
