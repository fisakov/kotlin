# Frontend Intermediate Representation (FIR)

Consider reading [fir-basics.md](../../docs/fir/fir-basics.md).

## FIR Compilation Phases

FIR processes code through sequential phases (see [`FirResolvePhase.kt`](tree/src/org/jetbrains/kotlin/fir/declarations/FirResolvePhase.kt)).

Key invariant: In phase B following phase A, all FIR elements visible in B are resolved to phase A.

## Inference

For type inference implementation details, read [inference.md](../../docs/fir/inference.md).

## Diagnostic Messages

When adding or changing diagnostic messages, follow the [Diagnostic Message Style Guide](../../docs/diagnostic_message_style_guide.md).

## Commit Guidelines

- **FIR prefix**: When changes are mostly related to FIR (`compiler/fir/`), use `FIR: ` prefix in the commit subject line.
- **Test-before-fix**: When fixing an issue and adding a test, commit the test data as a separate commit **before** the fix. The test in this commit must pass on current HEAD, i.e. it documents the current (buggy) behavior; the fix commit then updates the test data expectations. This helps reviewers see how the fix actually changes semantics (the test will show diagnostic differences in the fix commit).

## Testing

For FIR diagnostic test data format (directives, diagnostic markers, file structure), see [testData/diagnostics/AGENTS.md](../testData/diagnostics/AGENTS.md).
