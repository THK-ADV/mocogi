package database

import database.MyPostgresProfile.api.*

/** Snapshots for [[database.repo.ModuleUpdatePermissionRepository.modulesForPOs]]. */
final class GetModulesForPoSpec extends DatabaseSnapshotSuite {

  test("inf_inf2") {
    assertSnapshot("database/expected/get_modules_for_po/inf_inf2.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_po(ARRAY['inf_inf2']::text[])::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("inf_mi5") {
    assertSnapshot("database/expected/get_modules_for_po/inf_mi5.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_po(ARRAY['inf_mi5']::text[])::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("inf_wi5") {
    assertSnapshot("database/expected/get_modules_for_po/inf_wi5.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_po(ARRAY['inf_wi5']::text[])::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("inf_itm2") {
    assertSnapshot("database/expected/get_modules_for_po/inf_itm2.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_po(ARRAY['inf_itm2']::text[])::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("inf_itm2, inf_mi5, inf_wi5, inf_inf2") {
    assertSnapshot("database/expected/get_modules_for_po/inf_itm2_mi5_wi5_inf2.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_po(ARRAY['inf_itm2', 'inf_mi5', 'inf_wi5', 'inf_inf2']::text[])::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("inf_mim5") {
    assertSnapshot("database/expected/get_modules_for_po/inf_mim5.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_po(ARRAY['inf_mim5']::text[])::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("ing_een5") {
    assertSnapshot("database/expected/get_modules_for_po/ing_een5.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_po(ARRAY['ing_een5']::text[])::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("ing_wiw5") {
    assertSnapshot("database/expected/get_modules_for_po/ing_wiw5.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_po(ARRAY['ing_wiw5']::text[])::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("ing_gme5") {
    assertSnapshot("database/expected/get_modules_for_po/ing_gme5.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_po(ARRAY['ing_gme5']::text[])::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("ing_gme5, ing_een5, ing_wiw5") {
    assertSnapshot("database/expected/get_modules_for_po/ing_gme5_een5_wiw5.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_po(ARRAY['ing_gme5', 'ing_een5', 'ing_wiw5']::text[])::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("ing_gme5, ing_een5, ing_wiw5, inf_itm2, inf_mi5, inf_wi5, inf_inf2") {
    assertSnapshot("database/expected/get_modules_for_po/ing_gme5_een5_wiw5_inf_itm2_mi5_wi5_inf2.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_po(ARRAY['ing_gme5', 'ing_een5', 'ing_wiw5', 'inf_itm2', 'inf_mi5', 'inf_wi5', 'inf_inf2']::text[])::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("NULL po list => empty") {
    assertSnapshot("database/expected/get_modules_for_po/null.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_po(NULL::text[])::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("empty array => empty") {
    assertSnapshot("database/expected/get_modules_for_po/empty.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_po(ARRAY[]::text[])::jsonb)::text"""
        .as[String]
        .head
    }
  }
}
