package printing

import parsing.types.Content
import printer.Printer
import printer.Printer.{newline, prefix}
import printing.ModuleCompendiumPrinter._
import printing.PrintingLanguage.{English, German}

import javax.inject.Singleton

@Singleton
class ContentPrinter {

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

  def learningOutcome(lang: PrintingLanguage, text: String) =
    learningOutcomeHeader(lang)
      .skip(newline)
      .skip(prefix(text))

  def moduleContent(lang: PrintingLanguage, text: String) =
    moduleContentHeader(lang)
      .skip(newline)
      .skip(prefix(text))

  def teachingAndLearningMethods(lang: PrintingLanguage, text: String) =
    teachingAndLearningMethodsHeader(lang)
      .skip(newline)
      .skip(prefix(text))

  def recommendedReading(lang: PrintingLanguage, text: String) =
    recommendedReadingHeader(lang)
      .skip(newline)
      .skip(prefix(text))

  def particularities(lang: PrintingLanguage, text: String) =
    particularitiesHeader(lang)
      .skip(newline)
      .skip(prefix(text))

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
