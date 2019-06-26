package moe.yuuta.desktop

import kotlinx.cinterop.cValue
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import platform.posix.printf
import platform.windows.*
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

private data class WorkerArg(val desktopName: String,
                             val desktop: HDESK?,
                             val program: String?)

@Suppress("unused")
fun main(args: Array<String>?) {
    printf("Secure Desktop demo by i@yuuta.moe")
    val oldDesktop = GetThreadDesktop(GetCurrentThreadId())
    val desktopName = "desktop_dialog"
    val newDesktop = CreateDesktopA(desktopName,
        null,
        null,
        0.convert(),
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
    Worker.start().execute(TransferMode.SAFE,
        { WorkerArg(desktopName,
        newDesktop,
        if (args != null && args.isNotEmpty()) args[0] else null
    ) }) {
        exec(it)
    }.consume {
        SwitchDesktop(oldDesktop)
        CloseDesktop(newDesktop)
    }
}

private fun exec(arg: WorkerArg): Int {
    SetThreadDesktop(arg.desktop)
    if (arg.program != null) memScoped {
        val startupInfo = cValue<STARTUPINFOA> {
            lpDesktop = arg.desktopName.cstr.ptr
            dwFlags = STARTF_RUNFULLSCREEN.convert()
        }
        val pi = cValue<PROCESS_INFORMATION> {
        }
        // TODO: Close these objects?
        return@memScoped CreateProcessA(arg.program,
            null,
            null,
            null,
            0,
            0.convert(),
            null,
            null,
            startupInfo.ptr,
            pi.ptr)
    }
    return MessageBoxA(null,
        "Hi! This is message from thread ${GetCurrentThreadId()} in the secure desktop." +
                "\nTo load a custom program, run Desktop.exe <path\\to\\your\\program\\.exe>." +
                "\nYou can exit the desktop when you close the dialog or press OK. Programs in the desktop won't be terminated when you exit.",
        "Secure Desktop",
        (MB_OK or MB_ICONINFORMATION).toUInt())
}