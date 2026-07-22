# Module Catalog Config Overhaul

This plan is for agents implementing the module catalog and study-plan config overhaul.
Read it fully before changing code. Keep the implementation small, direct, and local to
the existing module-catalog flow.

## Engineering rules

- Preserve the current architecture unless a tiny helper makes the code clearly easier to read.
- Prefer less code over more code. Do not introduce a broad abstraction, provider, render-context wrapper, or framework-style layer unless the current implementation becomes obviously hard to follow.
- `ModuleCatalogService` should orchestrate data loading, config validation, and simple data preparation. LaTeX printer/snippets should print from the prepared view and should not re-interpret frontend semantics.
- Keep `StudyPlanSnippet` as the place that builds study-plan rows/tables unless moving logic to the service is plainly simpler.
- Use Scala braces for `class`, `object`, `trait`, and `def` blocks. Prefer clear Scala 2.13-style braced control flow over Scala 3 indentation syntax for newly written code.
- Make behavior explicit in small functions with precise names. Avoid clever generic models.

## Product decisions already settled

- Default behavior stays as currently implemented in the backend.
- Config stores only deviations from the default, not a full selected-module snapshot.
- Missing config fields mean "no override" and must parse as empty lists/options.
- Do not add config versioning.
- Do not use `part_of_catalog` for module-catalog rendering. It exists but is knowingly ignored here.
- Config must never override module master data. It only resolves ambiguity or instantiates generic study-plan placeholders for this generated artifact.
- All config references stay inside the generated PO. Invalid references are `400 Bad Request`.
- Successful generation has only warnings. Serious invalid config aborts with `400`; unexpected technical failures remain `500`.
- Preview PDF shows warnings on a diagnostic page. Final PDF does not show diagnostics; warnings are only logged.

## Suggested config shape

Use a small DTO with separate blocks. Exact names may change if the code reads better, but keep the semantics.

```json
{
  "moduleSelection": {
    "excludedModuleIds": [],
    "excludedElectiveOptions": [
      {
        "genericModuleId": "...",
        "optionModuleId": "..."
      }
    ]
  },
  "studyPlan": {
    "sections": [],
    "semesterSelections": [
      {
        "moduleId": "...",
        "selectedSemester": 5
      }
    ],
    "genericModuleOccurrences": [
      {
        "moduleId": "...",
        "semester": 5,
        "count": 2
      }
    ]
  }
}
```

Implementation notes:

- Keep old/small payloads working by defaulting missing objects and arrays to empty.
- If the current frontend still sends `bannedGenericModules`, either support it as a deprecated alias for `moduleSelection.excludedModuleIds` or coordinate the frontend migration in the same change.
- `excludedModuleIds` is a global artifact-level filter: after applying it, behave as if the module was never returned by the current PO preview query.
- `excludedElectiveOptions` is relation-specific: remove only the optional PO relationship where `optionModuleId` instantiates `genericModuleId` for the current PO. After removing that relationship, drop the concrete module if it no longer has any relationship to the current PO.

## Study-plan rules

- The study plan contains the mandatory curriculum only.
- For mandatory modules with multiple `recommendedSemester` values:
  - If `semesterSelections` contains the module, use the selected semester.
  - The selected semester must be one of the module's existing `recommendedSemester` values.
  - If no selection exists, keep current default behavior by using `min`, and collect a preview warning.
  - Do not change the "Empfohlenes Studiensemester" row in the module description.
- Generic-module occurrences:
  - Apply only to modules with type `generic_module`.
  - Used only for study-plan rows, not module descriptions.
  - If configured for a generic module, replace that module's default study-plan row completely.
  - Render multiple occurrences as multiple identical rows, without numbering.
  - `semester` must be one of the generic module's existing `recommendedSemester` values. If none exist, reject the config and require fixing the module data.
  - `count` must be positive.
- Excluded modules and removed elective relationships must affect both module descriptions and study-plan output because they are applied before printing.

## Specializations

- Specializations are rare; support them pragmatically without overengineering.
- If a PO has specializations, render the study plan as:
  1. one base table for non-specialized mandatory modules,
  2. one extension table per specialization, containing only that specialization's modules,
  3. each table has its own ECTS sum.
- Manual `sections` are only for POs without specializations. If a PO has specializations and the config contains `sections`, reject the config with `400 Bad Request`.
- For POs without specializations, keep the existing optional `sections` behavior.
- It is considered a data/modeling problem if the same module appears both in the base curriculum and a specialization. Do not build complex special handling for that case.

## Warnings and diagnostics

- Collect warnings during generation, close to where the fallback/default is actually used.
- Prefer a tiny structured warning type over raw strings, for example `code`, `message`, and optional `moduleId`. Keep it simple.
- Add a preview-only diagnostics LaTeX snippet/page. Put it immediately after the title page and before normal catalog content if possible.
- Warning examples:
  - mandatory module has multiple recommended semesters but no `semesterSelections` entry, so `min` was used,
  - generic module uses default single occurrence because no explicit occurrence override exists and that situation is worth surfacing,
  - mandatory module has no recommended semester and is therefore unassigned/omitted according to existing behavior.

## Options endpoint for the UI

- Add a dedicated options endpoint for the config mask, e.g. `GET /moduleCatalogs/:studyProgram/:po/configOptions`.
- Use the exact same data source as PDF preview generation: the preview Git module data currently loaded through `ModulePreview.getAllFromPreviewByPOWithLastModified`.
- The options DTO may be UI-friendly and richer than the config: labels, abbreviations, ECTS, module type, possible recommended semesters, default state, generic elective groups, option candidates, and specialization grouping.
- Do not compute warnings upfront in this endpoint. Warnings are generated during module-catalog rendering.
- Keep backend validation in the generate endpoint even if the UI uses the options endpoint correctly.

## Implementation checklist

1. Replace/extend `ModuleCatalogConfig` with additive nested config DTOs and custom `Reads` defaults.
2. Add config validation in `ModuleCatalogService` before printing:
   - unknown UUIDs,
   - references outside the current PO preview data,
   - elective option relationship does not exist,
   - semester selection not in the module's `recommendedSemester`,
   - generic occurrence for non-generic module,
   - generic occurrence semester not in `recommendedSemester`,
   - positive occurrence counts,
   - `sections` used together with PO specializations.
3. Apply module-selection overrides in the service by filtering modules and stripping excluded optional PO relationships from copied `ModuleProtocol` data.
4. Pass only the minimal new parameters into `ModuleCatalogLatexPrinter` and `StudyPlanSnippet`: filtered modules, study-plan config lists, specialization labels if needed, and a warning collector.
5. Update `StudyPlanSnippet` for semester selections, generic occurrences, preview warnings, and specialization extension tables.
6. Add preview-only diagnostic snippet/page and log warnings for final generation.
7. Add the config-options endpoint using preview Git data and the same auth/permission checks as generation.
8. Add focused tests:
   - config parsing defaults,
   - service filtering for excluded modules,
   - relation-specific elective option exclusion,
   - invalid config returns `400`,
   - selected semester must be one of the recommended semesters,
   - generic occurrences replace default rows and render duplicate rows,
   - sections rejected for specialized POs,
   - preview diagnostics appear only for preview.

## Non-goals

- Do not rewrite module catalog generation.
- Do not make a separate validation endpoint.
- Do not persist this config unless explicitly requested later.
- Do not make `part_of_catalog` meaningful for this feature.
- Do not introduce a versioned config format.
- Do not build elaborate support for pathological specialization/module overlap cases.
