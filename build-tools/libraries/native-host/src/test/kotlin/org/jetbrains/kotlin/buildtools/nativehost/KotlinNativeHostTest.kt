/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.nativehost

import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture.ARM64
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture.ARM32
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture.RISCV64
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture.X64
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture.X86
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs.FREEBSD
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs.LINUX
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs.MACOS
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs.UNKNOWN
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs.WINDOWS
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInNative
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Isolated("The HostManager comparison modifies system properties")
@OptIn(DelicateKotlinNativeHostApi::class)
class KotlinNativeHostTest {

    class Expected(
        val os: KotlinNativeHostOs,
        val architecture: KotlinNativeHostArchitecture,
        val targetName: String?,
        val name: String,
    )

    companion object {
        private val allTargets = listOf(
            "linux_x64", "linux_arm32_hfp", "linux_arm64", "mingw_x64",
            "android_x86", "android_x64", "android_arm32", "android_arm64",
            "macos_arm64", "ios_arm64", "ios_x64", "ios_simulator_arm64",
            "watchos_arm64", "watchos_x64", "watchos_simulator_arm64", "watchos_device_arm64",
            "tvos_arm64", "tvos_x64", "tvos_simulator_arm64",
        )

        private val appleTargets = allTargets.filter { target ->
            listOf("macos", "ios", "watchos", "tvos").any { target.startsWith(it) }
        }

        @JvmStatic
        fun detectedHosts(): List<Arguments> = listOf(
            Arguments.of("Mac OS X", "aarch64", Expected(MACOS, ARM64, "macos_arm64", "macos-aarch64")),
            Arguments.of("Mac OS X", "x86_64", Expected(MACOS, X64, null, "macos-x86_64")),
            Arguments.of("Linux", "amd64", Expected(LINUX, X64, "linux_x64", "linux-x86_64")),
            Arguments.of("Linux", "x86_64", Expected(LINUX, X64, "linux_x64", "linux-x86_64")),
            Arguments.of("Linux", "aarch64", Expected(LINUX, ARM64, null, "linux-aarch64")),
            Arguments.of("Linux", "i386", Expected(LINUX, X86, null, "linux-x86")),
            Arguments.of("Linux", "arm", Expected(LINUX, ARM32, null, "linux-arm")),
            Arguments.of("Linux", "riscv64", Expected(LINUX, RISCV64, null, "linux-riscv64")),
            Arguments.of("Linux", "ppc64le", Expected(LINUX, KotlinNativeHostArchitecture.UNKNOWN, null, "linux-ppc64le")),
            Arguments.of("Windows 11", "amd64", Expected(WINDOWS, X64, "mingw_x64", "windows-x86_64")),
            Arguments.of("Windows 11", "arm64", Expected(WINDOWS, ARM64, null, "windows-aarch64")),
            Arguments.of("Windows 11", "x86", Expected(WINDOWS, X86, null, "windows-x86")),
            Arguments.of("FreeBSD", "amd64", Expected(FREEBSD, X64, null, "freebsd-x86_64")),
            Arguments.of("SunOS", "aarch64", Expected(UNKNOWN, ARM64, null, "sunos-aarch64")),
        )

        private val unsupportedHosts = listOf(
            KotlinNativeHost.of(MACOS, X64),
            KotlinNativeHost.of(LINUX, ARM64),
            KotlinNativeHost.of(LINUX, RISCV64),
            KotlinNativeHost.of(WINDOWS, ARM64),
            KotlinNativeHost.of(FREEBSD, X64),
            KotlinNativeHost.of(UNKNOWN, X64),
            KotlinNativeHost.of(UNKNOWN, KotlinNativeHostArchitecture.UNKNOWN),
        )
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("detectedHosts")
    fun `detects the host from os name and arch`(osName: String, osArch: String, expected: Expected) {
        val host = KotlinNativeHost.detect(osName, osArch)
        assertEquals(expected.os, host.os)
        assertEquals(expected.architecture, host.architecture)
        assertEquals(expected.targetName, host.targetName)
        assertEquals(expected.targetName != null, host.isSupportedByKotlinNative)
        assertEquals(expected.name, host.name)
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("detectedHosts")
    fun `no member throws on any host`(osName: String, osArch: String, @Suppress("UNUSED_PARAMETER") expected: Expected) {
        val host = KotlinNativeHost.detect(osName, osArch)
        host.os; host.architecture; host.isSupportedByKotlinNative; host.targetName; host.name
        buildList { addAll(allTargets); add("no_such_target"); add("") }.forEach { target ->
            host.canLinkBinaries(target)
            host.canRunBinaries(target)
        }
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("detectedHosts")
    fun `detected host equals of with the same os and architecture`(osName: String, osArch: String, expected: Expected) {
        val host = KotlinNativeHost.detect(osName, osArch)
        val expectedHost = KotlinNativeHost.of(expected.os, expected.architecture)
        assertEquals(expectedHost, host)
        assertEquals(expectedHost.hashCode(), host.hashCode())
    }

    @Test
    fun `supportedHosts are exactly the hosts with a target`() {
        val supported = KotlinNativeHost.supportedHosts()
        assertEquals(listOf("macos_arm64", "linux_x64", "mingw_x64"), supported.map { it.targetName })
        assertTrue(supported.all { it.isSupportedByKotlinNative })
        KotlinNativeHostOs.entries.forEach { os ->
            KotlinNativeHostArchitecture.entries.forEach { arch ->
                val host = KotlinNativeHost.of(os, arch)
                assertEquals(host in supported, host.isSupportedByKotlinNative, host.name)
            }
        }
    }

    @Test
    fun `linux and windows hosts link every non-Apple target`() {
        val nonApple = allTargets - appleTargets.toSet()
        listOf(KotlinNativeHost.of(LINUX, X64), KotlinNativeHost.of(WINDOWS, X64)).forEach { host ->
            assertEquals(nonApple.toSet(), allTargets.filterTo(mutableSetOf()) { host.canLinkBinaries(it) }, host.name)
        }
    }

    @Test
    fun `macOS host links every target`() {
        val mac = KotlinNativeHost.of(MACOS, ARM64)
        assertEquals(allTargets.toSet(), allTargets.filterTo(mutableSetOf()) { mac.canLinkBinaries(it) })
        assertFalse(mac.canLinkBinaries("no_such_target"))
        assertFalse(mac.canLinkBinaries(""))
    }

    @Test
    fun `unsupported hosts link nothing`() {
        unsupportedHosts.forEach { host ->
            allTargets.forEach { target -> assertFalse(host.canLinkBinaries(target), "${host.name} must not link $target") }
        }
    }

    @Test
    fun `canRunBinaries covers the host target and matching simulators`() {
        val mac = KotlinNativeHost.of(MACOS, ARM64)
        val runnableOnMac = setOf("macos_arm64", "ios_simulator_arm64", "watchos_simulator_arm64", "tvos_simulator_arm64")
        assertEquals(runnableOnMac, allTargets.filterTo(mutableSetOf()) { mac.canRunBinaries(it) })

        val linux = KotlinNativeHost.of(LINUX, X64)
        assertEquals(setOf("linux_x64"), allTargets.filterTo(mutableSetOf()) { linux.canRunBinaries(it) })

        val windows = KotlinNativeHost.of(WINDOWS, X64)
        assertEquals(setOf("mingw_x64"), allTargets.filterTo(mutableSetOf()) { windows.canRunBinaries(it) })

        unsupportedHosts.forEach { host ->
            allTargets.forEach { target -> assertFalse(host.canRunBinaries(target), "${host.name} must not run $target") }
        }
    }

    @Test
    fun `of produces equal values that survive java serialization`() {
        val host = KotlinNativeHost.of(MACOS, ARM64)
        assertEquals(host, KotlinNativeHost.of(MACOS, ARM64))
        assertEquals(host.hashCode(), KotlinNativeHost.of(MACOS, ARM64).hashCode())

        val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).use { out -> out.writeObject(host) } }.toByteArray()
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() } as KotlinNativeHost
        assertEquals(host, restored)
        assertEquals("macos_arm64", restored.targetName)
        assertEquals("macos-aarch64", restored.name)
    }

    @Test
    fun `unsupported host has no target`() {
        val host = KotlinNativeHost.of(LINUX, ARM64)
        assertNull(host.targetName)
        assertFalse(host.isSupportedByKotlinNative)
        assertEquals(LINUX, host.os)
        assertEquals("linux-aarch64", host.name)
    }

    @MustRunOnChangesInNative
    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("detectedHosts")
    fun `current agrees with HostManager`(osName: String, osArch: String, @Suppress("UNUSED_PARAMETER") expected: Expected) {
        withSystemProperties(osName, osArch) {
            val host = KotlinNativeHost.current()
            assertEquals(HostManager.hostOrNull?.name, host.targetName)
            assertEquals(HostManager.hostIsSupported, host.isSupportedByKotlinNative)
            // HostManager keeps the case of an unknown `os.name`, the library lowercases it.
            // HostManager also keeps the raw `os.arch` of 32-bit x86 (`i386`, `i686`), the library names them all `x86`.
            val platformName = HostManager.platformName().lowercase()
            assertEquals(if (host.architecture == X86) platformName.replace(osArch, "x86") else platformName, host.name)
        }
    }

    @MustRunOnChangesInNative
    @Test
    fun `supported hosts and linkable targets agree with HostManager`() {
        val enabledByHost = HostManager().enabledByHost
        val supportedHosts = KotlinNativeHost.supportedHosts()
        assertEquals(enabledByHost.keys.map { it.name }.toSet(), supportedHosts.map { it.targetName }.toSet())

        val allKonanTargets = KonanTarget.predefinedTargets.values
        supportedHosts.forEach { host ->
            val enabled = enabledByHost.entries.single { it.key.name == host.targetName }.value
            assertEquals(
                enabled.map { it.name }.toSet(),
                allKonanTargets.map { it.name }.filterTo(mutableSetOf()) { host.canLinkBinaries(it) },
                host.name,
            )
        }
    }

    @MustRunOnChangesInNative
    @Test
    fun `every Apple target of HostManager is classified for canRunBinaries`() {
        // A new Apple target must be added either here or to the simulators that canRunBinaries knows.
        val notRunnableOnMac = setOf(
            "ios_arm64", "watchos_arm64", "watchos_device_arm64", "tvos_arm64",
            "ios_x64", "watchos_x64", "tvos_x64",
        )
        val mac = KotlinNativeHost.of(MACOS, ARM64)
        val appleTargets = HostManager().enabledByHost.entries.single { it.key.name == mac.targetName }.value
            .filter { it.family.isAppleFamily }
            .map { it.name }
        val runnableOnMac = appleTargets.filterTo(mutableSetOf()) { mac.canRunBinaries(it) }
        assertEquals(emptySet(), runnableOnMac intersect notRunnableOnMac)
        assertEquals(appleTargets.toSet(), runnableOnMac + notRunnableOnMac)
    }

    @Test
    fun `equal hosts have equal names unless a part is unknown`() {
        assertEquals(KotlinNativeHost.of(LINUX, X86).name, KotlinNativeHost.detect("Linux", "i686").name)
        assertEquals("freebsd-arm", KotlinNativeHost.detect("FreeBSD", "arm").name)
        assertEquals("freebsd-arm", KotlinNativeHost.of(FREEBSD, ARM32).name)
        assertEquals("unknown-unknown", KotlinNativeHost.of(UNKNOWN, KotlinNativeHostArchitecture.UNKNOWN).name)
    }

    @Test
    fun `unknown name parts are safe to use in file names`() {
        assertEquals("unknown-aarch64", KotlinNativeHost.detect("", "aarch64").name)
        assertEquals("unknown-unknown", KotlinNativeHost.detect(null, null).name)
        assertEquals("z_os-s390x", KotlinNativeHost.detect("z/OS", "s390x").name)
        assertEquals("hp_ux-pa_risc2_0", KotlinNativeHost.detect("HP-UX", "PA_RISC2.0").name)
    }

    @Test
    fun `hashCode does not depend on the JVM run`() {
        assertEquals(31 * LINUX.ordinal + X64.ordinal, KotlinNativeHost.of(LINUX, X64).hashCode())
    }

    private fun withSystemProperties(osName: String, osArch: String, block: () -> Unit) {
        val backup = mapOf("os.name" to System.getProperty("os.name"), "os.arch" to System.getProperty("os.arch"))
        try {
            System.setProperty("os.name", osName)
            System.setProperty("os.arch", osArch)
            block()
        } finally {
            backup.forEach { (key, value) -> if (value == null) System.clearProperty(key) else System.setProperty(key, value) }
        }
    }
}
