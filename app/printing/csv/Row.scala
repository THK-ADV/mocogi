package printing.csv

private[csv] case class Row(
    semester: String,
    module: String,
    moduleNumber: String,
    submodule: String,
    moduleType: String,
    submoduleCredits: String,
    totalCredits: String,
    attendanceRequirement: String,
    attendanceRequirementText: String,
    attendanceRequirementJustification: String,
    assessmentPrerequisite: String,
    assessmentPrerequisiteText: String,
    assessmentPrerequisiteJustification: String,
    assessmentMethods: String,
    assessmentMethodsCount: String
) {
  def toList: List[String] =
    List(
      semester,
      module,
      moduleNumber,
      submodule,
      moduleType,
      submoduleCredits,
      totalCredits,
      attendanceRequirement,
      attendanceRequirementText,
      attendanceRequirementJustification,
      assessmentPrerequisite,
      assessmentPrerequisiteText,
      assessmentPrerequisiteJustification,
      assessmentMethods,
      assessmentMethodsCount
    )
}
