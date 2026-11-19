# Design for `KotlinNativeHost`, a build tool independent description of the build host

## Glossary

* **Host.** The machine the build runs on, described by its operating system and CPU architecture.
* **Supported host.** A host that has a Kotlin/Native distribution: `macos-aarch64`, `linux-x86_64` and `windows-x86_64`.
  See [Kotlin/Native target support](https://kotlinlang.org/docs/native-target-support.html#hosts).
* **Host target.** The Kotlin/Native target whose binaries run on the host itself, for example `macos_arm64` on an Apple Silicon Mac.
* **Target name.** The Kotlin/Native name of a target, as used in `konan.properties` and in the `konanTargetAttribute` of KGP:
  `macos_arm64`, `linux_x64`, `mingw_x64` and so on.
* **`HostManager`.** `org.jetbrains.kotlin.konan.target.HostManager` from `kotlin-native-utils`, an internal Kotlin/Native class
  that build tools and build scripts use today to answer questions about the host.

## References

* [KT-91830](https://youtrack.jetbrains.com/issue/KT-91830) Add a Kotlin Build Tools library describing the Kotlin/Native host
* [KT-39994](https://youtrack.jetbrains.com/issue/KT-39994) Multiplatform Gradle plugin: Public API for getting host info
* [KT-86717](https://youtrack.jetbrains.com/issue/KT-86717) `HostManager.host` throws at configuration time on linux-aarch64
* [KT-64512](https://youtrack.jetbrains.com/issue/KT-64512) Rework `HostManager.enabled`
* [KT-66699](https://youtrack.jetbrains.com/issue/KT-66699), [KT-27114](https://youtrack.jetbrains.com/issue/KT-27114) `HostManager` ABI breaks
  that hit Gradle plugins
* [KT-70247](https://youtrack.jetbrains.com/issue/KT-70247) Remove compiler modules from KGP runtime
* [KT-84955](https://youtrack.jetbrains.com/issue/KT-84955) Deprecate Apple x86_64 targets as Error, which also dropped the Intel Mac host

---

## TL;DR

* We add `KotlinNativeHost`, a small immutable value that describes the build host. It lives in a new Kotlin Build Tools library,
  `build-tools/libraries/native-host`, published as `org.jetbrains.kotlin:build-tools-native-host`.
* Its main job is to tell the OS and the architecture of the host. The enums cover the common hosts, including those
  Kotlin/Native does not support.
* It also tells what Kotlin/Native supports on the host: whether there is a distribution, which target is the host target, and
  which targets the host can link or run. This part is a copy of `HostManager` data, so it requires opt-in with
  `@DelicateKotlinNativeHostApi`.
* Nothing in it throws, and it depends only on the stdlib. KGP, the Maven plugin, JPS, kotlin-toolchain and Bazel rules can use it
  without `kotlin-native-utils`.
* We added a member only when we found a use for it, in KGP sources or in a GitHub survey of about 500 user build scripts and
  plugins.
* Using the library in KGP and exposing it to build scripts is a separate change (KT-39994).

## Problem

### What is the current state?

Build tools that work with Kotlin/Native need to know the host. KGP asks `HostManager`, an internal Kotlin/Native class in
`kotlin-native-utils`. KGP main sources call it in 61 places across 34 files.

Build scripts and third-party plugins call it too, because `kotlin-gradle-plugin-api` exposes `kotlin-native-utils` as an `api`
dependency. A GitHub code search finds about 430 Kotlin build scripts, 70 Groovy scripts and 770 plugin or `buildSrc` sources that
reference it. Among them are ktor, sqldelight, androidx, aws-crt-kotlin, smithy-kotlin, kotlinx-io, kotlinx-benchmark, KSP,
binary-compatibility-validator, SKIE, sentry-kmp, multipaz and intellij-community.

### What is the problem?

* `HostManager` throws on unsupported hosts. `host`, `hostName` and `HostManager().isEnabled()` throw `TargetSupportException`
  on linux-aarch64, FreeBSD, Windows on ARM and any other host without a Kotlin/Native distribution. `hostOs()` throws on an
  unknown OS such as FreeBSD, and `hostArch()` on an unknown architecture such as riscv64. In Gradle, one eager call in any
  subproject fails the whole build, even when it runs only JVM tasks (KT-86717). Users work around it with reflection
  (CompleteKotlin), with `try/catch TargetSupportException` (binary-compatibility-validator) or by parsing `os.name` themselves
  (boringssl-kmp, filament-kmp).
* Its API does not match what users ask. `isEnabled` is one boolean that means "can link binaries". Users who need "can run
  tests" or "can cross-link mingw from macOS" write the check by hand and often get it wrong (KT-64512).
* Its ABI is not stable. Constructor changes broke published plugins twice (KT-27114, KT-66699), and the Kotlin/Native team wants
  to stay free to change the class.
* It lives in a compiler module. KT-70247 removes `kotlin-native-utils` from the KGP runtime, and other build tools would have to
  pull the module in only to learn the host.

### What is this proposal solving?

Build tool integrations get a documented description of the host that they can use instead of `HostManager`.

### Why is it important?

KT-91830 and KT-39994 are planned for 2.5.20. Builds keep failing on hosts that are new to CI, and linux-aarch64 runners are
common now. Today we can only tell users to stop using `HostManager`, and we have nothing to offer in its place.

## Anti-Goals

This proposal does not try to:

* Be a general OS detection library. The enums cover the common hosts so that build logic can branch on them, and nothing more.
  The uses unrelated to Kotlin/Native that we saw (Compose Desktop package formats, JNI library paths, `where` versus
  `command -v`) are served well enough by `System.getProperty("os.name")`.
* Provision toolchains. Downloading `kotlin-native-prebuilt` stays with the build tools, and the toolchain work in KT-70247 decides
  how that evolves.
* Change `HostManager` or `kotlin-native-utils`. Kotlin/Native keeps its class as is.
* Answer project-level questions. Whether a klib can be compiled for a target depends on project settings and cinterops, so the
  build tool has to answer that.
* Follow the Kotlin/Native version selected for the build. See [Kotlin/Native support](#kotlinnative-support-opt-in).

## Solution (overview)

```kotlin
import org.jetbrains.kotlin.buildtools.nativehost.DelicateKotlinNativeHostApi
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHost
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs

val host = KotlinNativeHost.current()

// Host information
if (host.os == KotlinNativeHostOs.MACOS) {
    // Run Xcode tooling
}
if (host.architecture == KotlinNativeHostArchitecture.RISCV64) {
    logger.warn("No prebuilt binaries for ${host.name}")
}

// Kotlin/Native support, requires opt-in
@OptIn(DelicateKotlinNativeHostApi::class)
fun configureNative(host: KotlinNativeHost) {
    when (host.targetName) {
        "macos_arm64" -> { /* ... */ }
        null -> logger.warn("Kotlin/Native does not support ${host.name}")
    }

    host.canLinkBinaries("ios_arm64")   // true only on a Mac
    host.canRunBinaries("linux_x64")    // true only on Linux x86_64
}
```

## Solution

### `KotlinNativeHost`

```kotlin
// build-tools/libraries/native-host
package org.jetbrains.kotlin.buildtools.nativehost

public sealed interface KotlinNativeHost : java.io.Serializable {
    public val os: KotlinNativeHostOs
    public val architecture: KotlinNativeHostArchitecture
    public val name: String               // "macos-aarch64"

    @DelicateKotlinNativeHostApi public val isSupportedByKotlinNative: Boolean
    @DelicateKotlinNativeHostApi public val targetName: String?        // "macos_arm64", "linux_x64", "mingw_x64" or null
    @DelicateKotlinNativeHostApi public fun canLinkBinaries(targetName: String): Boolean
    @DelicateKotlinNativeHostApi public fun canRunBinaries(targetName: String): Boolean

    public companion object {
        @JvmStatic public fun current(): KotlinNativeHost
        @JvmStatic public fun of(os: KotlinNativeHostOs, architecture: KotlinNativeHostArchitecture): KotlinNativeHost
        @JvmStatic @DelicateKotlinNativeHostApi public fun supportedHosts(): List<KotlinNativeHost>
    }
}

public enum class KotlinNativeHostOs { MACOS, LINUX, WINDOWS, FREEBSD, UNKNOWN }
public enum class KotlinNativeHostArchitecture { X86, X64, ARM32, ARM64, RISCV64, UNKNOWN }

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
public annotation class DelicateKotlinNativeHostApi
```

The members fall into two groups:

* Host information: `os`, `architecture` and `name`. These are facts about the machine. They do not depend on Kotlin/Native and
  need no opt-in.
* Kotlin/Native support: `isSupportedByKotlinNative`, `targetName`, `canLinkBinaries`, `canRunBinaries` and `supportedHosts()`.
  The library copies this data from `HostManager`, and these members require opt-in.

No member throws. An unknown or unsupported host is an ordinary value: the enums have `UNKNOWN`, `targetName` is `null` and the
checks return `false`. This is what KT-86717 asks for.

The library depends only on the stdlib, so it refers to targets by name. A build tool that has `kotlin-native-utils` can map the
names to `KonanTarget`. KGP does that in `kotlin-gradle-plugin-api` (KT-39994).

The type lives in `build-tools/libraries` because every build tool needs the same answer: the Kotlin Gradle plugin, the Maven
plugin, JPS, kotlin-toolchain and Bazel rules. Since it does not depend on `kotlin-native-utils`, KT-70247 does not affect it.

### Host information

#### `os`

Replaces `hostIsMac`, `hostIsLinux`, `hostIsMingw`, `hostOs()`, `simpleOsName()` and `host.family`.

OS checks are the most common use of `HostManager`. `hostIsMac` alone accounts for 23 of the 61 call sites in KGP and is the
most used member in the GitHub survey. What users do with it:

* declare Apple targets only on macOS (algolia, multipaz, kotlinxtras);
* skip Swift and Xcode tasks on other hosts (spm4Kmp, kotlin-docc, moko-mvvm, and in KGP `FatFrameworkTask`, CocoaPods and Swift
  Export);
* skip `cinterop` on Windows (smithy-kotlin);
* choose between `.zip` and `.tar.gz` for the distribution (`NativeCompilerDownloader`).

An enum removes the `"osx"` versus `"macos"` confusion of `hostOs()`. For example, xmlutil compares `hostOs()` with `"macos"`,
which never matches.

The enum has the three operating systems Kotlin/Native supports and `FREEBSD`, the unsupported OS that appears in our issues
(KT-74277, KT-75741, KT-28079). `UNKNOWN` covers everything else. We can add entries to both enums later, so a `when` over them
needs an `else` branch. A host that is `UNKNOWN` today can get its own entry in a later release.

`os` describes the operating system only, and that differs from `HostManager`. `hostIsMac`, `hostIsLinux` and `hostIsMingw` are
`false` on every unsupported host, while `os` is `MACOS` on an Intel Mac (KT-84955), `LINUX` on linux-aarch64 and `WINDOWS` on
Windows on ARM. A build tool that needs a working Kotlin/Native toolchain on macOS checks
`os == MACOS && isSupportedByKotlinNative`.

The library has no `isMacOs`, `isLinux` or `isWindows` shortcuts. KGP can add them as extensions in `kotlin-gradle-plugin-api`
(KT-39994).

#### `architecture`

Replaces `hostArch()`, `hostArchOrNull()` and `host.architecture`.

Build tools use the architecture to pick the right target for the host (`macosArm64` or `macosX64`, `linuxArm64` or `linuxX64`)
and to decide whether a simulator test can run on this machine. KGP does the latter in `KotlinNativeTestRunFactories`, and
intellij-community in its test run line marker. xmlutil's `ideaPreset(host)`, KGP's `DisabledNativeCacheTest` and cklib's
`hostArch` do the same with strings today.

The enum has `X86`, `X64`, `ARM32`, `ARM64` and `RISCV64`. Users reported failures on each of the unsupported ones: KT-50726
on x86, KT-48566 on 32-bit ARM and KT-77977 on riscv64. `UNKNOWN` covers the rest, for example ppc64le.

#### `name`

Replaces `platformName()`, the hand-made `simpleOsName()` plus `hostArch()` concatenations, and `hostName` where it is only
displayed.

A host id in the form `<os>-<architecture>`: `macos-aarch64`, `linux-x86_64`, `windows-x86_64`. On a supported host it is the
classifier in `kotlin-native-prebuilt-<version>-<name>`. `NativeCompilerDownloader`, SKIE and intellij-community build that
classifier today, and CompleteKotlin gets it through reflection. KGP also prints the name in its diagnostics about unsupported
hosts.

Every enum entry except `UNKNOWN` has a fixed name part, so equal hosts have equal names: FreeBSD on amd64 is `freebsd-x86_64`,
Linux on riscv64 is `linux-riscv64`, and Linux on an `i386` or `i686` JVM is `linux-x86`. An `UNKNOWN` part is the lowercased
`os.name` or `os.arch` value, for example `sunos-aarch64` or `linux-ppc64le`. Characters other than letters, digits and `_` are
replaced with `_`, so `z/OS` becomes `z_os` and the name is safe in a file name. The part is `unknown` when the value is empty
or the host was created with `of()`. Because of the `UNKNOWN` case the parts are not always fixed, and callers should not parse
the name.

`platformName()` returns the same strings on supported hosts. On other hosts it differs in two ways: it keeps the case of an
unknown `os.name` (`FreeBSD-x86_64`), and it keeps the raw `os.arch` of 32-bit x86 (`linux-i386`).

`name` is not part of equality. Two hosts are equal when their `os` and `architecture` are equal.

#### `current()`

Replaces the `HostManager` companion and `HostManager()` instantiation.

`current()` reads `os.name` and `os.arch`, so it describes the running JVM. An x64 JVM under Rosetta on an Apple Silicon Mac
is detected as `MACOS` and `X64`, which Kotlin/Native does not support. `HostManager` behaves the same.

It reads the properties on every call and caches nothing. Reading two system properties is cheap, and Gradle
records the reads as configuration cache inputs. A cache would cause problems: the Gradle daemon reuses plugin classloaders
between builds, and tests change the system properties to simulate other hosts.

#### `of(os, architecture)`

`HostManager` has no counterpart. Plugin authors who want to test both branches of a host check have to pass the host through
their code as a parameter, such as `hostIsMac: Boolean = HostManager.hostIsMac` (sentry-kmp, SKIE's `KgpShim`). With `of()` they
can create any host in a test without changing system properties.

### Kotlin/Native support (opt-in)

`isSupportedByKotlinNative`, `targetName`, `canLinkBinaries`, `canRunBinaries` and `supportedHosts()` tell what Kotlin/Native
supports on a host. The library copies this data from `HostManager`, and that has two consequences.

The copy can drift from the original. To catch that, `KotlinNativeHostTest` has a test-only dependency on `kotlin-native-utils`
and compares the library with `HostManager`. `HostManager` has no data about simulators, so for `canRunBinaries` the test
checks instead that every Apple target of `HostManager` is listed as runnable or not runnable on a Mac. A new target fails the
test until someone decides. The published artifact still depends only on the stdlib.

The copy describes one Kotlin/Native version, the one the library was released with. A build can select another version, with
`kotlin.native.version` today and through BTA later, and then the answers can be wrong. For example, an older Kotlin/Native still
supports the Intel Mac host, and the library says it does not. KGP has the same limitation today, because it uses the
`HostManager` bundled with KGP.

For these reasons the members are marked with `@DelicateKotlinNativeHostApi`. It is an opt-in annotation of `WARNING` level,
like `@DelicateKotlinGradlePluginApi`. An answer that follows the selected version has to come from the toolchain itself through
BTA. `NativePlatformToolchain` can only compile klibs today, so that is a follow-up. The opt-in lets us move these members there
later.

#### `isSupportedByKotlinNative`

Replaces `hostIsSupported`, `hostOrNull != null` and `try { host } catch (TargetSupportException)`.

It tells whether a Kotlin/Native distribution exists for the host. KGP asks this in eight places: publishing, the commonizer,
platform dependencies, target creation and checkers. Users ask it before they touch anything native: KSP,
binary-compatibility-validator, CrossPaste, and convention plugins that skip native targets on linux-aarch64 CI.

#### `targetName`

Replaces `host`, `hostOrNull`, `hostName` and `host.name`.

The name of the host target, for example `macos_arm64`. After OS checks, this is the second most common use of `HostManager`.
`konanTargetAttribute` values and `konan.properties` keys use the same names. On an unsupported host it is `null`, like
`hostOrNull`, and there is no variant that throws.

Users need it to:

* declare the host target (hfhbd, kotlinxtras, parsus, lemcoder, kotlinx-io);
* resolve host klibs through `konanTargetAttribute` (KMP-NativeCoroutines, atomicfu, serialization);
* check for cross-compilation (cryptography-kotlin, the intellij run configuration producer).

KGP uses it for the `cacheableTargets` property keys in `KonanPropertiesBuildService` and to find the target preset of the current
host.

#### `canLinkBinaries(targetName)`

Replaces `HostManager().isEnabled(target)`, `HostManager().enabled` and `enabledByHost`.

It tells whether the host can link final binaries for a target. It does not cover klib compilation. On an unsupported host and
for an unknown target name it returns `false`.

Users often write this check by hand (aws-crt-kotlin, CrossPaste). Other call sites:

* `cinterop` and CMake conditions (aws-crt-kotlin, kotlin-sqlite-bindings, powersync);
* per-target ABI dumps in binary-compatibility-validator;
* the host check in KSP;
* in KGP: `PlatformLibrariesGenerator`, `CinteropCrossCompilationChecker`, FUS metrics and enabling link tasks.

The data is a copy of the `HostManager.enabledByHost` table.

#### `canRunBinaries(targetName)`

Replaces `target == HostManager.host` for host targets and `hostIsMac && host.architecture == target.architecture` for simulator
targets (`KotlinNativeTestRunFactories` in KGP).

It tells whether binaries of a target, such as test executables, can run on the host. That is `true` for the host target and, on
macOS, for the simulator targets with the host's architecture. The only supported macOS host is ARM64, so the x64 simulators
(`ios_x64`, `watchos_x64`, `tvos_x64`) never run. intellij-community uses the same rules for run line markers.

`HostManager` has no such method. Users disable `linuxX64Test` and `linkDebugTestLinuxX64` by comparing OS booleans instead
(sqldelight, SQLiter, FileKit, kzmq).

#### `supportedHosts()`

Replaces the internal `HostManager.supportedHosts` extension and `enabledByHost.keys`.

The list of hosts that have a Kotlin/Native distribution. KGP builds two things from it: the "supported hosts are ..." line of
its diagnostics, and the set of targets that every host can link. It returns `KotlinNativeHost` values and not strings, so that
callers do not start to depend on the `name` format.

### What is not included

`targetManager()`, `resolveAlias()`, `listAliases()`, `jniHostPlatformIncludeDir` and `defaultJvmArgs` serve the compiler CLI. We
found no build tool that uses them.

### Migration from `HostManager`

| Legacy API | Replacement | Notes |
|---|---|---|
| `hostIsMac`, `hostIsLinux`, `hostIsMingw` | `os` | the old getters are `false` on unsupported hosts, `os` is not |
| `hostOs()`, `simpleOsName()` | `os` | enum instead of `"osx"` strings |
| `hostArch()`, `hostArchOrNull()` | `architecture` | |
| `platformName()` | `name` | differs on unsupported hosts: lower case, and `x86` for `i386` and `i686` |
| `hostIsSupported`, `hostOrNull != null` | `isSupportedByKotlinNative` | opt-in |
| `host`, `hostOrNull` | `targetName` | opt-in; callers must handle `null` |
| `hostName`, `host.name` | `targetName` | opt-in |
| `HostManager().isEnabled(t)`, `.enabled`, `.enabledByHost` | `canLinkBinaries(t)` | opt-in |
| `HostManager.supportedHosts`, `enabledByHost.keys` | `supportedHosts()` | opt-in |

### Design properties

* `KotlinNativeHost` is a value. It is `Serializable`, stores two enums and the `name` string, and has no references to build
  tool objects. A Gradle task can capture it, and the configuration cache can store it.
* There is no static state. `current()` reads the system properties on every call.
* It works from Java and Groovy. `current()`, `of()` and `supportedHosts()` are `@JvmStatic`, and `isSupportedByKotlinNative`
  follows bean naming, so Groovy reads `host.supportedByKotlinNative`. Opt-in is a Kotlin feature, so Java and Groovy callers get
  no warning for the Kotlin/Native members.
* Equal hosts have equal hash codes in every JVM run, because `hashCode()` is built from enum ordinals.
* We can add members later. The interface is `sealed`, so nobody outside the library can implement it, and `current()` and
  `of()` are the only factories.
* The ABI dump records the public API, and `checkKotlinAbi` fails when it changes without an updated dump.

### Rollout

* 2.5.20: the library ships as `org.jetbrains.kotlin:build-tools-native-host`. KGP starts to use it and exposes it to build
  scripts in the same release (KT-39994).
* Later: other build tool integrations move their host checks to the library, and BTA gets a query for what the selected
  Kotlin/Native version supports.

## Testing

`KotlinNativeHostTest` runs without any build tool. Detection is tested through an internal function that takes `os.name` and
`os.arch` as arguments, so most tests do not touch system properties:

| `os.name` / `os.arch` | `os` | `architecture` | `targetName` | `name` |
|---|---|---|---|---|
| `Mac OS X` / `aarch64` | `MACOS` | `ARM64` | `macos_arm64` | `macos-aarch64` |
| `Mac OS X` / `x86_64` | `MACOS` | `X64` | `null` | `macos-x86_64` |
| `Linux` / `amd64`, `x86_64` | `LINUX` | `X64` | `linux_x64` | `linux-x86_64` |
| `Linux` / `aarch64` | `LINUX` | `ARM64` | `null` | `linux-aarch64` |
| `Linux` / `i386` | `LINUX` | `X86` | `null` | `linux-x86` |
| `Linux` / `arm` | `LINUX` | `ARM32` | `null` | `linux-arm` |
| `Linux` / `riscv64` | `LINUX` | `RISCV64` | `null` | `linux-riscv64` |
| `Linux` / `ppc64le` | `LINUX` | `UNKNOWN` | `null` | `linux-ppc64le` |
| `Windows 11` / `amd64` | `WINDOWS` | `X64` | `mingw_x64` | `windows-x86_64` |
| `Windows 11` / `arm64` | `WINDOWS` | `ARM64` | `null` | `windows-aarch64` |
| `Windows 11` / `x86` | `WINDOWS` | `X86` | `null` | `windows-x86` |
| `FreeBSD` / `amd64` | `FREEBSD` | `X64` | `null` | `freebsd-x86_64` |
| `SunOS` / `aarch64` | `UNKNOWN` | `ARM64` | `null` | `sunos-aarch64` |

A host is supported exactly when `targetName` is not `null`. Some rows come from issues: the Intel Mac host was dropped in
KT-84955, linux-aarch64 is KT-86717, riscv64 is KT-77977, and FreeBSD is KT-74277 and KT-75741.

The test also checks that:

* no member throws for any row of the table;
* `supportedHosts()` lists exactly the hosts with a target, and `isSupportedByKotlinNative` agrees with it for every OS and
  architecture;
* Linux and Windows hosts link every non-Apple target, a Mac links every target, and unsupported hosts link nothing;
* `canRunBinaries` is `true` for the host target and, on a Mac, for the simulator targets with the host's architecture, and `false`
  for every target on unsupported hosts;
* `of()` produces equal values that survive Java serialization, and a detected host equals `of()` with the same OS and
  architecture;
* equal hosts have equal names when no part is `UNKNOWN`, and an `UNKNOWN` part is safe in a file name.

Three tests compare the library with `HostManager`. They are marked with `@MustRunOnChangesInNative`, so they also run before
a change in Kotlin/Native code merges. The first one sets the system properties, because `HostManager` reads them:

* for every row of the table, `current()` agrees with `HostManager.hostOrNull`, `hostIsSupported` and `platformName()`, apart
  from the two known differences in `name`;
* `supportedHosts()` and `canLinkBinaries` agree with `HostManager.enabledByHost` for every `KonanTarget`;
* every Apple target in `HostManager.enabledByHost` is listed as runnable or not runnable on a Mac.

## Related work

* [KT-39994](https://youtrack.jetbrains.com/issue/KT-39994) moves KGP from `HostManager` to this library and adds a build script DSL
  on top of it.
* [kotlin-native-toolchain](https://github.com/adamko-dev/kotlin-native-toolchain) by Adam Semenenko provisions distributions outside
  KGP and generates dependency data. It does not describe the host, so it does not overlap with this library. Its
  `knt-dependency-data` could later replace the classifier logic in `NativeCompilerDownloader`, which uses `name`.

# Prior art

**Possible alternatives:**

* Use Gradle's `org.gradle.nativeplatform.platform.OperatingSystem`. It belongs to the Gradle native plugins, which Gradle no
  longer develops, and it does not exist outside Gradle. It knows nothing about Kotlin/Native targets or about which targets a
  host can link, and it covers about a third of the call sites in the survey.
* Fix `HostManager` itself: add the missing hosts and stop throwing. Kotlin/Native does not want the class to become a public
  contract, and every change to it has to be checked against all existing usages. The ABI problem stays, and the class stays in a
  compiler module.
* Keep only host information in the library and wait for BTA to provide the Kotlin/Native part. That would leave KT-39994 without
  `targetName` and `canLinkBinaries`, which are among the most used members in the survey. We keep them and require opt-in.
* Adopt `knt-dependency-data` from kotlin-native-toolchain. It covers provisioning and dependency metadata and does not describe
  the host.

If we do nothing, builds keep failing on new CI hosts as in KT-86717, and `HostManager` changes keep breaking plugins as in
KT-66699.

## FAQ

**Q:** Why target names and not `KonanTarget`?

**A:** `KonanTarget` lives in `kotlin-native-utils`, a compiler module. With target names the library does not need that module,
so any build tool can use it without the compiler. Build tools that have `KonanTarget` can map names to it. KGP adds such
extensions in `kotlin-gradle-plugin-api`.

**Q:** Why copy the table of linkable targets instead of delegating to `HostManager`?

**A:** So that build tools can drop `kotlin-native-utils` without a behaviour change.

**Q:** How do we keep the copy consistent with `HostManager`?

**A:** Tests in the library compare the two, so a new host or target in `HostManager` without a change in the library fails
the library's `check`. See [Testing](#testing).

**Q:** What if the build uses another Kotlin/Native version than the one the library was released with?

**A:** The Kotlin/Native members can then be wrong. KGP has this limitation with `HostManager` today. The members require opt-in
for this reason, and the long-term answer is a BTA query to the selected toolchain. See
[Kotlin/Native support](#kotlinnative-support-opt-in).

**Q:** Why is `supportedHosts()` a list of hosts and not of strings?

**A:** With strings, callers would depend on the `name` format. With typed values they can produce their own text, and they
can compute the set of targets that every host can link without a second copy of the host list.

**Q:** Why is there no cache in `current()`?

**A:** Reading two system properties is cheap. A cache would outlive a build in the Gradle daemon and would break tests that
simulate other hosts.
