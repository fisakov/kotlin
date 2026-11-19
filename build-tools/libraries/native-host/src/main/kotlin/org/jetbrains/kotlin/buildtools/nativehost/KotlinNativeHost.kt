/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.nativehost

import java.io.Serializable
import java.util.Locale

/**
 * The machine the build runs on, as the JVM reports it: its [os] and [architecture].
 *
 * It can also tell what Kotlin/Native supports on this host, e.g. [targetName] or [canLinkBinaries]. Those members
 * are marked with [DelicateKotlinNativeHostApi], because they describe the Kotlin/Native version this library was
 * released with.
 *
 * Unlike `HostManager` from `kotlin-native-utils`, nothing here throws on hosts without a Kotlin/Native distribution
 * (e.g. Linux ARM64). Hosts are equal if their [os] and [architecture] are equal.
 *
 * Get instances from [current] or [of].
 */
public sealed interface KotlinNativeHost : Serializable {

    /** The operating system of the host. */
    public val os: KotlinNativeHostOs

    /** The CPU architecture of the host. */
    public val architecture: KotlinNativeHostArchitecture

    /**
     * Whether a Kotlin/Native distribution exists for this host.
     *
     * See [the list of supported hosts](https://kotlinlang.org/docs/native-target-support.html#hosts).
     */
    @DelicateKotlinNativeHostApi
    public val isSupportedByKotlinNative: Boolean

    /**
     * The Kotlin/Native target of this host, e.g. `macos_arm64`, or `null` if the host is not supported.
     * Same as `HostManager.hostOrNull?.name`.
     */
    @DelicateKotlinNativeHostApi
    public val targetName: String?

    /**
     * A host id in the form `<os>-<architecture>`, e.g. `macos-aarch64` or `freebsd-x86_64`.
     *
     * On hosts with a Kotlin/Native distribution it is the classifier of `kotlin-native-prebuilt`. An `UNKNOWN` OS or
     * architecture is the `os.name` or `os.arch` value in lower case, with characters other than letters, digits
     * and `_` replaced by `_`. It is `unknown` if the value is empty or the host was created with [of]. So don't
     * parse the name.
     */
    public val name: String

    /**
     * Whether this host can link binaries for the given target, e.g. `ios_arm64`. Apple targets need a macOS host.
     * Same as `HostManager().isEnabled(target)`, but `false` instead of an exception on unsupported hosts.
     */
    @DelicateKotlinNativeHostApi
    public fun canLinkBinaries(targetName: String): Boolean

    /**
     * Whether binaries of the given target (e.g. tests) can run on this host: the host target itself and,
     * on macOS, simulators of the host architecture.
     */
    @DelicateKotlinNativeHostApi
    public fun canRunBinaries(targetName: String): Boolean

    /** Factories of [KotlinNativeHost]. */
    public companion object {
        /**
         * The current host, detected from the `os.name` and `os.arch` system properties. These describe the running
         * JVM: an x64 JVM on an Apple Silicon Mac gives [KotlinNativeHostArchitecture.X64].
         */
        @JvmStatic
        public fun current(): KotlinNativeHost = detect(System.getProperty("os.name"), System.getProperty("os.arch"))

        // Detection without system properties, so that tests don't have to change them.
        internal fun detect(osName: String?, osArch: String?): KotlinNativeHost = DefaultKotlinNativeHost(
            os = KotlinNativeHostOs.detect(osName.orEmpty()),
            architecture = KotlinNativeHostArchitecture.detect(osArch.orEmpty()),
            rawOsName = osName.orEmpty(),
            rawOsArch = osArch.orEmpty(),
        )

        /** A host with the given [os] and [architecture]. Useful for testing host-dependent logic. */
        @JvmStatic
        public fun of(os: KotlinNativeHostOs, architecture: KotlinNativeHostArchitecture): KotlinNativeHost =
            DefaultKotlinNativeHost(os, architecture, rawOsName = "", rawOsArch = "")

        /** The hosts Kotlin/Native ships a distribution for. */
        @JvmStatic
        @DelicateKotlinNativeHostApi
        public fun supportedHosts(): List<KotlinNativeHost> = listOf(
            of(KotlinNativeHostOs.MACOS, KotlinNativeHostArchitecture.ARM64),
            of(KotlinNativeHostOs.LINUX, KotlinNativeHostArchitecture.X64),
            of(KotlinNativeHostOs.WINDOWS, KotlinNativeHostArchitecture.X64),
        )
    }
}

/**
 * Host operating system. Covers the common ones, not only those Kotlin/Native supports.
 *
 * New entries can be added, so keep an `else` branch in `when`.
 */
public enum class KotlinNativeHostOs {
    /** `os.name` is `Mac OS X`. */
    MACOS,

    /** `os.name` is `Linux`. */
    LINUX,

    /** `os.name` starts with `Windows`. */
    WINDOWS,

    /** `os.name` is `FreeBSD`. */
    FREEBSD,

    /** Anything else, for example Solaris. */
    UNKNOWN;

    internal companion object {
        internal fun detect(osName: String): KotlinNativeHostOs = when {
            osName == "Mac OS X" -> MACOS
            osName == "Linux" -> LINUX
            osName.startsWith("Windows") -> WINDOWS
            osName == "FreeBSD" -> FREEBSD
            else -> UNKNOWN
        }
    }
}

/**
 * Host CPU architecture. Covers the common ones, not only those Kotlin/Native supports.
 *
 * New entries can be added, so keep an `else` branch in `when`.
 */
public enum class KotlinNativeHostArchitecture {
    /** `os.arch` is `x86`, `i386`, `i486`, `i586` or `i686`. */
    X86,

    /** `os.arch` is `x86_64` or `amd64`. */
    X64,

    /** `os.arch` is `arm`. */
    ARM32,

    /** `os.arch` is `aarch64` or `arm64`. */
    ARM64,

    /** `os.arch` is `riscv64`. */
    RISCV64,

    /** Anything else, for example ppc64le. */
    UNKNOWN;

    internal companion object {
        internal fun detect(osArch: String): KotlinNativeHostArchitecture = when (osArch) {
            "x86", "i386", "i486", "i586", "i686" -> X86
            "x86_64", "amd64" -> X64
            "arm" -> ARM32
            "arm64", "aarch64" -> ARM64
            "riscv64" -> RISCV64
            else -> UNKNOWN
        }
    }
}

@OptIn(DelicateKotlinNativeHostApi::class)
private class DefaultKotlinNativeHost(
    override val os: KotlinNativeHostOs,
    override val architecture: KotlinNativeHostArchitecture,
    rawOsName: String,
    rawOsArch: String,
) : KotlinNativeHost {

    // The only stored state is the two enums and `name`, so the value serializes anywhere.
    override val targetName: String?
        get() = when (os to architecture) {
            KotlinNativeHostOs.MACOS to KotlinNativeHostArchitecture.ARM64 -> "macos_arm64"
            KotlinNativeHostOs.LINUX to KotlinNativeHostArchitecture.X64 -> "linux_x64"
            KotlinNativeHostOs.WINDOWS to KotlinNativeHostArchitecture.X64 -> "mingw_x64"
            else -> null
        }

    override val isSupportedByKotlinNative: Boolean get() = targetName != null

    // Each part falls back on its own: a known OS keeps its name next to an unknown architecture (`linux-ppc64le`).
    override val name: String = listOf(
        os.namePart ?: rawOsName.toNamePart(),
        architecture.namePart ?: rawOsArch.toNamePart(),
    ).joinToString("-")

    override fun canLinkBinaries(targetName: String): Boolean {
        val host = this.targetName ?: return false
        return targetName in linkableTargetsByHost.getValue(host)
    }

    override fun canRunBinaries(targetName: String): Boolean {
        val host = this.targetName ?: return false
        if (targetName == host) return true
        return os == KotlinNativeHostOs.MACOS && simulatorArchitectures[targetName] == architecture
    }

    override fun equals(other: Any?): Boolean =
        other is DefaultKotlinNativeHost && os == other.os && architecture == other.architecture

    // Ordinals and not the enum hash codes, which differ between JVM runs.
    override fun hashCode(): Int = 31 * os.ordinal + architecture.ordinal

    override fun toString(): String = "KotlinNativeHost($name)"

    // A raw `os.name` or `os.arch` value can be empty or contain characters like `/` (`z/OS`).
    private fun String.toNamePart(): String =
        lowercase(Locale.ROOT).replace(unsafeNameCharacters, "_").ifEmpty { "unknown" }

    // macOS, Linux, Windows, x86_64 and aarch64 are named as in the Kotlin/Native distribution classifier.
    private val KotlinNativeHostOs.namePart: String?
        get() = when (this) {
            KotlinNativeHostOs.MACOS -> "macos"
            KotlinNativeHostOs.LINUX -> "linux"
            KotlinNativeHostOs.WINDOWS -> "windows"
            KotlinNativeHostOs.FREEBSD -> "freebsd"
            KotlinNativeHostOs.UNKNOWN -> null
        }

    private val KotlinNativeHostArchitecture.namePart: String?
        get() = when (this) {
            KotlinNativeHostArchitecture.X86 -> "x86"
            KotlinNativeHostArchitecture.X64 -> "x86_64"
            KotlinNativeHostArchitecture.ARM32 -> "arm"
            KotlinNativeHostArchitecture.ARM64 -> "aarch64"
            KotlinNativeHostArchitecture.RISCV64 -> "riscv64"
            KotlinNativeHostArchitecture.UNKNOWN -> null
        }

    companion object {
        @Suppress("unused") // read by Java serialization
        private const val serialVersionUID = 1L

        private val unsafeNameCharacters = Regex("[^a-z0-9_]+")

        // Targets each supported host can link. Copied from HostManager.enabledByHost in kotlin-native-utils;
        // KotlinNativeHostTest checks they agree.
        private val commonTargets: Set<String> = setOf(
            "linux_x64",
            "linux_arm32_hfp",
            "linux_arm64",
            "mingw_x64",
            "android_x86",
            "android_x64",
            "android_arm32",
            "android_arm64",
        )

        private val appleTargets: Set<String> = setOf(
            "macos_arm64",
            "ios_arm64",
            "ios_x64",
            "ios_simulator_arm64",
            "watchos_arm64",
            "watchos_x64",
            "watchos_simulator_arm64",
            "watchos_device_arm64",
            "tvos_arm64",
            "tvos_x64",
            "tvos_simulator_arm64",
        )

        private val linkableTargetsByHost: Map<String, Set<String>> = mapOf(
            "linux_x64" to commonTargets,
            "mingw_x64" to commonTargets,
            "macos_arm64" to commonTargets + appleTargets,
        )

        // Apple simulator targets and the host architecture they run on. The x64 simulators are not listed,
        // because Kotlin/Native has no Intel Mac host. KotlinNativeHostTest fails when a new Apple target appears.
        private val simulatorArchitectures: Map<String, KotlinNativeHostArchitecture> = mapOf(
            "ios_simulator_arm64" to KotlinNativeHostArchitecture.ARM64,
            "watchos_simulator_arm64" to KotlinNativeHostArchitecture.ARM64,
            "tvos_simulator_arm64" to KotlinNativeHostArchitecture.ARM64,
        )
    }
}
