# Compiler Architecture

## Frontend

**K2/FIR (Current)**: Located in `compiler/fir/` - Frontend IR, the new compiler frontend

Read [AGENTS.md](fir/AGENTS.md).

## IR (Intermediate Representation)

Located in `compiler/ir/`. Backend IR is used by all targets for:
- Lowering (transforming code to target-friendly form)
- Optimization
- Serialization to klibs

Backend implementations:
- `compiler/ir/backend.jvm/` - JVM backend
- `compiler/ir/backend.js/` - JavaScript backend
- `compiler/ir/backend.wasm/` - WebAssembly backend
- `kotlin-native/backend.native/`, `native/` - Native backend

