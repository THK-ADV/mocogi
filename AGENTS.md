# Agent Coding Guidance

This repository prefers straightforward, maintainable code over clever abstractions.

## Core principles

- Prefer simplicity over complex architecture.
- Keep modules deep where useful, but do not add indirection without clear value.
- Minimize new types/providers unless they improve correctness or clarity.

## Scala style

- Prefer expression-oriented Scala.
- Use braces for `class` / `object` / `trait` / `def` blocks.
- For control flow:
  - Prefer `match { ... }` with braces around the `match` block.
  - In `match` cases, avoid extra braces around case bodies unless needed.
  - For `try/catch`, prefer `try <expr> catch { case ... => ... }` when `try` is a single expression.
  - For function bodies that are single expressions, prefer `def f(...) = expr` (no extra braces).
  - Keep case bodies and throw expressions readable via indentation (Scala 2.13 style feel).

## Dependency injection

- Prefer injecting the smallest dependency needed by a component.
- For single config values, prefer direct Guice bindings (for example `@Named` + `toInstance`) over injecting large config aggregates when practical.
