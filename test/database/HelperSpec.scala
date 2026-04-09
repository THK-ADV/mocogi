package database

import database.MyPostgresProfile.api.*

final class HelperSpec extends DatabaseSnapshotSuite {

  test("resolve responsibilities for pp") {
    assertSnapshot("database/expected/helper/resolve_responsibilities_pp.txt") {
      sql"""SELECT jsonb_pretty(to_jsonb(t.*))::text FROM modules.resolve_responsibilities('e37c5af9-6076-4f15-8c8b-d206b7091bc0'::uuid) t"""
        .as[String]
        .head
    }
  }

  test("resolve responsibilities for fsios") {
    assertSnapshot("database/expected/helper/resolve_responsibilities_fsios.txt") {
      sql"""SELECT jsonb_pretty(to_jsonb(t.*))::text FROM modules.resolve_responsibilities('6d7e31f7-0b9e-4162-be4e-89a977c0a9ed'::uuid) t"""
        .as[String]
        .head
    }
  }

    test("resolve assessment methods for pp") {
    assertSnapshot("database/expected/helper/resolve_assessment_methods_pp.txt") {
      sql"""SELECT jsonb_pretty(modules.resolve_assessment_methods('e37c5af9-6076-4f15-8c8b-d206b7091bc0'::uuid)::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("resolve assessment methods for fsios") {
    assertSnapshot("database/expected/helper/resolve_assessment_methods_fsios.txt") {
      sql"""SELECT jsonb_pretty(modules.resolve_assessment_methods('6d7e31f7-0b9e-4162-be4e-89a977c0a9ed'::uuid)::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("resolve po relationships for pp") {
    assertSnapshot("database/expected/helper/resolve_po_relationships_pp.txt") {
      sql"""SELECT jsonb_pretty(to_jsonb(t.*))::text FROM modules.resolve_po_relationships('e37c5af9-6076-4f15-8c8b-d206b7091bc0'::uuid) t"""
        .as[String]
        .head
    }
  }
  
  test("resolve po relationships for fsios") {
    assertSnapshot("database/expected/helper/resolve_po_relationships_fsios.txt") {
      sql"""SELECT jsonb_pretty(to_jsonb(t.*))::text FROM modules.resolve_po_relationships('6d7e31f7-0b9e-4162-be4e-89a977c0a9ed'::uuid) t"""
        .as[String]
        .head
    }
  }

  test("resolve module relation for pp") {
    assertSnapshot("database/expected/helper/resolve_module_relation_pp.txt") {
      sql"""SELECT coalesce(jsonb_pretty(modules.resolve_module_relation('e37c5af9-6076-4f15-8c8b-d206b7091bc0'::uuid)::jsonb)::text, 'null')"""
        .as[String]
        .head
    }
  }

  test("resolve module relation for iug (child)") {
    assertSnapshot("database/expected/helper/resolve_module_relation_iug.txt") {
      sql"""SELECT jsonb_pretty(modules.resolve_module_relation('05674322-071c-4a3a-8d8b-3c21c6bb640c'::uuid)::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("resolve module relation for irg (parent)") {
    assertSnapshot("database/expected/helper/resolve_module_relation_irg.txt") {
      sql"""SELECT jsonb_pretty(modules.resolve_module_relation('e3dc0278-cf5f-4296-a577-d88ad9c3e999'::uuid)::jsonb)::text"""
        .as[String]
        .head
    }
  }
}
