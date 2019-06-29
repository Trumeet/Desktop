plugins {
    kotlin("multiplatform") version "1.3.40"
}
repositories {
    jcenter()
}

kotlin {
    mingwX86("desktop").binaries.executable {
        entryPoint("moe.yuuta.desktop.main")
        linkerOpts("-Wl,--subsystem,windows")
        windowsResources("desktop.rc")
    }
    val desktopMain by sourceSets.getting {
        dependencies {
            implementation("com.github.msink:libui:0.1.4")
        }
    }
}

fun org.jetbrains.kotlin.gradle.plugin.mpp.Executable.windowsResources(rcFileName: String) {
    val taskName = linkTaskName.replaceFirst("link", "windres")
    val inFile = compilation.defaultSourceSet.resources.sourceDirectories.singleFile.resolve(rcFileName)
    val outFile = buildDir.resolve("processedResources/$taskName.res")

    val windresTask = tasks.create<Exec>(taskName) {
        val konanUserDir = System.getenv("KONAN_DATA_DIR") ?: "${System.getProperty("user.home")}/.konan"
        val konanLlvmDir = "$konanUserDir/dependencies/msys2-mingw-w64-i686-gcc-7.4.0-clang-llvm-6.0.1/bin"

        inputs.file(inFile)
        outputs.file(outFile)
        commandLine("$konanLlvmDir/windres", inFile, "-D_${buildType.name}", "-O", "coff", "-o", outFile)
        environment("PATH", "$konanLlvmDir;${System.getenv("PATH")}")

        dependsOn(compilation.compileKotlinTask)
    }

    linkTask.dependsOn(windresTask)
    linkerOpts(outFile.toString())
}