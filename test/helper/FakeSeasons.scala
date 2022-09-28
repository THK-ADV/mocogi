package helper

import parsing.types.Season

trait FakeSeasons {
  implicit def fakeSeasons: Seq[Season] = Seq(
    Season("ws", "Wintersemester", "--"),
    Season("ss", "Sommersemester", "--"),
    Season("ws_ss", "Winter- und Sommersemester", "--")
  )
}
