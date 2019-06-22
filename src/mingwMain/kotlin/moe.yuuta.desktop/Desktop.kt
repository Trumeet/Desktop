package moe.yuuta.desktop

import platform.posix.printf
import platform.windows.*
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

@Suppress("unused")
fun main() {
    printf("Secure Desktop demo by i@yuuta.moe")
    val oldDesktop = GetThreadDesktop(GetCurrentThreadId())
    val newDesktop = CreateDesktopA("desktop_dialog",
        null,
        null,
        0,
        (DESKTOP_READOBJECTS or
                DESKTOP_CREATEWINDOW or
                DESKTOP_CREATEMENU or
                DESKTOP_HOOKCONTROL or
                DESKTOP_JOURNALRECORD or
                DESKTOP_JOURNALPLAYBACK or
                DESKTOP_ENUMERATE or
                DESKTOP_WRITEOBJECTS or
                DESKTOP_SWITCHDESKTOP).toUInt(),
        null)
    SwitchDesktop(newDesktop)
    Worker.start().execute(TransferMode.SAFE, { newDesktop }) {
        SetThreadDesktop(it)
        MessageBoxA(null,
            "Hi! This is message from thread ${GetCurrentThreadId()} in the secure desktop.",
            "Secure Desktop",
            (MB_OK or MB_ICONINFORMATION).toUInt())
    }.consume {
        SwitchDesktop(oldDesktop)
        CloseDesktop(newDesktop)
    }
}