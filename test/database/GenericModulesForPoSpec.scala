package database

import database.MyPostgresProfile.api.*

/** Snapshots for [[database.repo.JSONRepository.genericModulesForPO]]. */
final class GenericModulesForPoSpec extends DatabaseSnapshotSuite {

  test("PO inf_inf2") {
    assertSnapshot("database/expected/generic_modules_for_po/inf_inf2.txt") {
      sql"""SELECT jsonb_pretty(modules.generic_modules_for_po('inf_inf2')::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("PO inf_mi5") {
    assertSnapshot("database/expected/generic_modules_for_po/inf_mi5.txt") {
      sql"""SELECT jsonb_pretty(modules.generic_modules_for_po('inf_mi5')::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("PO inf_wi5") {
    assertSnapshot("database/expected/generic_modules_for_po/inf_wi5.txt") {
      sql"""SELECT jsonb_pretty(modules.generic_modules_for_po('inf_wi5')::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("PO inf_itm2") {
    assertSnapshot("database/expected/generic_modules_for_po/inf_itm2.txt") {
      sql"""SELECT jsonb_pretty(modules.generic_modules_for_po('inf_itm2')::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("PO inf_mim5") {
    assertSnapshot("database/expected/generic_modules_for_po/inf_mim5.txt") {
      sql"""SELECT jsonb_pretty(modules.generic_modules_for_po('inf_mim5')::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("PO ing_gme5") {
    assertSnapshot("database/expected/generic_modules_for_po/ing_gme5.txt") {
      sql"""SELECT jsonb_pretty(modules.generic_modules_for_po('ing_gme5')::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("PO ing_wiw5") {
    assertSnapshot("database/expected/generic_modules_for_po/ing_wiw5.txt") {
      sql"""SELECT jsonb_pretty(modules.generic_modules_for_po('ing_wiw5')::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("PO ing_een5") {
    assertSnapshot("database/expected/generic_modules_for_po/ing_een5.txt") {
      sql"""SELECT jsonb_pretty(modules.generic_modules_for_po('ing_een5')::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("unknown PO => empty") {
    assertSnapshot("database/expected/generic_modules_for_po/unknown_po.txt") {
      sql"""SELECT jsonb_pretty(modules.generic_modules_for_po('inf_abc')::jsonb)::text"""
        .as[String]
        .head
    }
  }
}
