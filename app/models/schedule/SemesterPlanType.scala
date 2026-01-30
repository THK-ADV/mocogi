package models.schedule

enum SemesterPlanType(val id: String) {
  case Exam           extends SemesterPlanType("exam")
  case Lecture        extends SemesterPlanType("lecture")
  case Block          extends SemesterPlanType("block")
  case Project        extends SemesterPlanType("project")
  case ClosedBuilding extends SemesterPlanType("closed_building")
  case SelfStudy      extends SemesterPlanType("self_study")
}

object SemesterPlanType {
  def apply(id: String) =
    id.toLowerCase match {
      case "exam" => Exam
      case "lecture" => Lecture
      case "block" => Block
      case "project" => Project
      case "closed_building" => ClosedBuilding
      case "self_study" => SelfStudy
    }
}
