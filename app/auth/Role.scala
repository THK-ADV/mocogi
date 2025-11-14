package auth

@deprecated
enum Role(val label: String):
  case AccessDraftBranch extends Role("access-draft-branch")
  case Admin             extends Role("admin")
