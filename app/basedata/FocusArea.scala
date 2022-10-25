package basedata

case class FocusArea(
    abbrev: String,
    program: StudyProgramPreview,
    deLabel: String,
    enLabel: String,
    deDesc: String,
    enDesc: String
) extends AbbrevLabelDescLike
