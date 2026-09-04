package printing.latex

import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Matcher

import scala.collection.mutable.ListBuffer
import scala.util.Try

import ops.FileOps.rename

final class TextIntroRewriter {

  private val prefix = "intro_"

  private val tableEnd       = "\\end{longtable}"
  private val tableBodyStart = "\\endlastfoot"
  private val longtable      = """(?s)\\begin\{longtable\}.*?\\end\{longtable\}""".r
  private val multirow       = """\\multirow(?:\[[^]]*])?\{(\d+)\}""".r
  private val multicolumn    = """\\multicolumn\{(\d+)\}""".r
  private val minipage       = """\\(begin|end)\{minipage\}""".r

  def rewrite(texFile: Path): Try[Path] =
    Try(rename(rewriteTexFile(texFile)))

  private def rename(path: Path): Path =
    path.rename(prefix + path.getFileName.toString)

  /** Force figures to render at their defined position */
  private def figureFloat: PartialFunction[String, String] = {
    case line if line.startsWith("\\begin{figure}") => "\\begin{figure}[H]"
  }

  /** Normalize image width and reject unsupported .emf files */
  private def fixImage: PartialFunction[String, String] = {
    case line if line.startsWith("\\includegraphics") =>
      if line.contains(".emf") then "\\textbf{unable to include .emf image file}"
      else {
        val img = line.dropWhile(_ != ']')
        s"\\includegraphics[width=1.0\\textwidth$img"
      }
  }

  /** Remove redundant "Abbildung"/"Tabelle" prefix from captions */
  private def stripCaptionPrefix: PartialFunction[String, String] = {
    case line if line.startsWith("\\caption{") =>
      line.replaceFirst(
        """\\caption\{(Abbildung|Tabelle)(?:\s*:?\s*\d+(?:\.\d+)*(?=\s|:)\s*:?\s*|\s+:\s*)""",
        "\\\\caption{"
      )
  }

  /** Pass through unmatched lines unchanged */
  private def identity: PartialFunction[String, String] = { case line => line }

  private def rewriteLine: String => String =
    figureFloat.orElse(fixImage).orElse(stripCaptionPrefix).orElse(identity)

  private def rewriteTexFile(path: Path): Path = {
    val rewrite = Files.readString(path).linesIterator.map(rewriteLine).mkString("\n")
    Files.writeString(
      path,
      longtable.replaceAllIn(rewrite, table => Matcher.quoteReplacement(rewriteTable(table.matched)))
    )
  }

  private def rewriteTable(table: String): String = {
    val bodyStart = table.indexOf(tableBodyStart)
    if bodyStart < 0 || table.indexOf("\\multirow", bodyStart) < 0 then table
    else {
      val offset       = bodyStart + tableBodyStart.length
      val body         = table.substring(offset, table.length - tableEnd.length)
      val (rows, tail) = splitRows(body)
      if rows.size < 2 then table
      else {
        val cells       = rows.map(splitCells)
        val columnCount = cells.map(_.map(columnSpan).sum).max
        val activeSpans = Array.fill(columnCount)(0)
        val result      = new StringBuilder(table.substring(0, offset))

        rows.zip(cells).zipWithIndex.foreach {
          case ((row, rowCells), index) =>
            activeSpans.indices.foreach(column => activeSpans(column) = math.max(0, activeSpans(column) - 1))
            var column = 0
            rowCells.foreach { cell =>
              val width = columnSpan(cell)
              multirow.findFirstMatchIn(cell).foreach { rowSpan =>
                (column until math.min(column + width, columnCount))
                  .foreach(activeSpans(_) = rowSpan.group(1).toInt - 1)
              }
              column += width
            }
            result.append(row).append('\n')
            if index < rows.size - 1 then result.append(partialRules(activeSpans)).append('\n')
        }
        result.append(tail).append(tableEnd).result()
      }
    }
  }

  private def splitRows(body: String): (List[String], String) = {
    val rows      = ListBuffer[String]()
    val row       = new StringBuilder()
    var depth     = 0
    var minipages = 0
    body.linesIterator.foreach { line =>
      row.append(line).append('\n')
      depth += braceDepthChange(line)
      minipage.findAllMatchIn(line).foreach(m => minipages += (if m.group(1) == "begin" then 1 else -1))
      if depth == 0 && minipages == 0 && line.trim.endsWith("\\\\") then {
        rows += row.result().stripSuffix("\n")
        row.clear()
      }
    }
    rows.toList -> row.result()
  }

  private def splitCells(row: String): List[String] = {
    val cells   = ListBuffer[String]()
    var start   = 0
    var depth   = 0
    var escaped = false
    row.indices.foreach { index =>
      row(index) match {
        case _ if escaped      => escaped = false
        case '\\'              => escaped = true
        case '{'               => depth += 1
        case '}'               => depth -= 1
        case '&' if depth == 0 =>
          cells += row.substring(start, index)
          start = index + 1
        case _ =>
      }
    }
    cells += row.substring(start)
    cells.toList
  }

  private def braceDepthChange(value: String): Int = {
    var depth   = 0
    var escaped = false
    value.foreach {
      case _ if escaped => escaped = false
      case '\\'         => escaped = true
      case '{'          => depth += 1
      case '}'          => depth -= 1
      case _            =>
    }
    depth
  }

  private def columnSpan(cell: String): Int =
    multicolumn.findFirstMatchIn(cell).fold(1)(_.group(1).toInt)

  private def partialRules(activeSpans: Array[Int]): String =
    activeSpans.indices
      .filter(activeSpans(_) == 0)
      .map(_ + 1)
      .foldLeft(List.empty[(Int, Int)]) {
        case ((start, end) :: tail, column) if column == end + 1 => (start, column) :: tail
        case (ranges, column)                                    => (column, column) :: ranges
      }
      .reverse
      .map { case (start, end) => s"\\cmidrule(lr){$start-$end}" }
      .mkString
}
