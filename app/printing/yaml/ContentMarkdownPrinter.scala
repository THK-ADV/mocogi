package printing.yaml

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

import parsing.types.ModuleContent
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import printer.Printer
import printer.Printer.newline
import printer.Printer.prefix
import printing.LocalizedStrings

@Singleton
private[printing] final class ContentMarkdownPrinter @Inject() (messages: MessagesApi) {

  private def abbrev(string: LocalizedStrings) =
    if string.isGerman then "de" else "en"

  private def learningOutcomeHeader(string: LocalizedStrings) =
    prefix(s"## (${abbrev(string)}) ${string.learningOutcomeMarkdownLabel}:")

  private def moduleContentHeader(string: LocalizedStrings) =
    prefix(s"## (${abbrev(string)}) ${string.moduleContentMarkdownLabel}:")

  private def teachingAndLearningMethodsHeader(string: LocalizedStrings) =
    prefix(s"## (${abbrev(string)}) ${string.teachingAndLearningMethodsMarkdownLabel}:")

  private def recommendedReadingHeader(string: LocalizedStrings) =
    prefix(s"## (${abbrev(string)}) ${string.recommendedReadingMarkdownLabel}:")

  private def particularitiesHeader(string: LocalizedStrings) =
    prefix(s"## (${abbrev(string)}) ${string.particularitiesMarkdownLabel}:")

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

  def learningOutcome(string: LocalizedStrings, text: String) =
    content(learningOutcomeHeader(string), text)

  def moduleContent(string: LocalizedStrings, text: String) =
    content(moduleContentHeader(string), text)

  def teachingAndLearningMethods(string: LocalizedStrings, text: String) =
    content(teachingAndLearningMethodsHeader(string), text)

  def recommendedReading(string: LocalizedStrings, text: String) =
    content(recommendedReadingHeader(string), text)

  def particularities(string: LocalizedStrings, text: String) =
    content(particularitiesHeader(string), text)

  def printer(): Printer[(ModuleContent, ModuleContent)] =
    Printer {
      case ((de, en), input) =>
        val deStrings = new LocalizedStrings(messages)(using Lang(Locale.GERMAN))
        val enStrings = new LocalizedStrings(messages)(using Lang(Locale.ENGLISH))

        learningOutcome(deStrings, de.learningOutcome)
          .skip(newline)
          .skip(learningOutcome(enStrings, en.learningOutcome))
          .skip(newline)
          .skip(
            moduleContent(deStrings, de.content)
              .skip(newline)
              .skip(moduleContent(enStrings, en.content))
              .skip(newline)
          )
          .skip(
            teachingAndLearningMethods(deStrings, de.teachingAndLearningMethods)
              .skip(newline)
              .skip(
                teachingAndLearningMethods(enStrings, en.teachingAndLearningMethods)
              )
              .skip(newline)
          )
          .skip(
            recommendedReading(deStrings, de.recommendedReading)
              .skip(newline)
              .skip(recommendedReading(enStrings, en.recommendedReading))
              .skip(newline)
          )
          .skip(
            particularities(deStrings, de.particularities)
              .skip(newline)
              .skip(particularities(enStrings, en.particularities))
          )
          .print((), input)
    }
}
