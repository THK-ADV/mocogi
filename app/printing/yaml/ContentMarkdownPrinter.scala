package printing.yaml

import parsing.types.Content
import printer.Printer
import printer.Printer.{newline, prefix}
import printing.PrintingLanguage
import printing.PrintingLanguage.{English, German}

import javax.inject.Singleton

@Singleton
class ContentMarkdownPrinter {

  private def abbrev(lang: PrintingLanguage) =
    lang match {
      case PrintingLanguage.German  => "de"
      case PrintingLanguage.English => "en"
    }

  def learningOutcomeHeader(lang: PrintingLanguage) =
    prefix(s"## (${abbrev(lang)}) ${lang.learningOutcomeLabel}:")

  def moduleContentHeader(lang: PrintingLanguage) =
    prefix(s"## (${abbrev(lang)}) ${lang.moduleContentLabel}:")

  def teachingAndLearningMethodsHeader(lang: PrintingLanguage) =
    prefix(s"## (${abbrev(lang)}) ${lang.teachingAndLearningMethodsLabel}:")

  def recommendedReadingHeader(lang: PrintingLanguage) =
    prefix(s"## (${abbrev(lang)}) ${lang.recommendedReadingLabel}:")

  def particularitiesHeader(lang: PrintingLanguage) =
    prefix(s"## (${abbrev(lang)}) ${lang.particularitiesLabel}:")

  private def content(header: Printer[Unit], text: String) =
    if (text.isEmpty) {
      header
        .skip(newline)
        .skip(prefix(text))
    } else {
      header
        .skip(newline.repeat(2))
        .skip(prefix(text))
        .skip(newline)
    }

  def learningOutcome(lang: PrintingLanguage, text: String) =
    content(learningOutcomeHeader(lang), text)

  def moduleContent(lang: PrintingLanguage, text: String) =
    content(moduleContentHeader(lang), text)

  def teachingAndLearningMethods(lang: PrintingLanguage, text: String) =
    content(teachingAndLearningMethodsHeader(lang), text)

  def recommendedReading(lang: PrintingLanguage, text: String) =
    content(recommendedReadingHeader(lang), text)

  def particularities(lang: PrintingLanguage, text: String) =
    content(particularitiesHeader(lang), text)

  def printer(): Printer[(Content, Content)] =
    Printer { case ((de, en), input) =>
      learningOutcome(German, de.learningOutcome)
        .skip(newline)
        .skip(learningOutcome(English, en.learningOutcome))
        .skip(newline)
        .skip(
          moduleContent(German, de.content)
            .skip(newline)
            .skip(moduleContent(English, en.content))
            .skip(newline)
        )
        .skip(
          teachingAndLearningMethods(German, de.teachingAndLearningMethods)
            .skip(newline)
            .skip(
              teachingAndLearningMethods(English, en.teachingAndLearningMethods)
            )
            .skip(newline)
        )
        .skip(
          recommendedReading(German, de.recommendedReading)
            .skip(newline)
            .skip(recommendedReading(English, en.recommendedReading))
            .skip(newline)
        )
        .skip(
          particularities(German, de.particularities)
            .skip(newline)
            .skip(particularities(English, en.particularities))
        )
        .print((), input)
    }
}
