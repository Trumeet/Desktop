package moe.yuuta.desktop

import kotlinx.cinterop.cValue
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import platform.posix.printf
import platform.windows.*
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

private data class WorkerArg(val desktop: InternalDesktop,
                             val program: String?)

/**
 * Worker does not allow transferring external references.
 */
private data class InternalDesktop(
    val desktop: HDESK,
    val name: String?
)

@Suppress("unused")
fun main(args: Array<String>?) {
    printf("Secure Desktop demo by i@yuuta.moe")
    val oldDesktop = WinApi.getThreadDesktop()
    val desktopName = "desktop_dialog"
    val newDesktop = WinApi.create(desktopName,
        DESKTOP_READOBJECTS or
                DESKTOP_CREATEWINDOW or
                DESKTOP_CREATEMENU or
                DESKTOP_HOOKCONTROL or
                DESKTOP_JOURNALRECORD or
                DESKTOP_JOURNALPLAYBACK or
                DESKTOP_ENUMERATE or
                DESKTOP_WRITEOBJECTS or
                DESKTOP_SWITCHDESKTOP)
    if (newDesktop == null) {
        MessageBoxA(null,
            "Cannot create the desktop. API returned null.",
            "Secure Desktop",
            (MB_OK or MB_ICONERROR).toUInt())
        return
    }
    if (oldDesktop == null) {
        MessageBoxA(null,
            "Cannot obtain the current desktop. API returned null.",
            "Secure Desktop",
            (MB_OK or MB_ICONERROR).toUInt())
        return
    }
    WinApi.switch(newDesktop)
    Worker.start().execute(TransferMode.SAFE,
        { WorkerArg(
            InternalDesktop(newDesktop.desktop,
                newDesktop.name),
        if (args != null && args.isNotEmpty()) args[0] else null
    ) }) {
        exec(it)
    }.consume {
        WinApi.switch(oldDesktop)
        WinApi.close(newDesktop)
    }
}

private fun exec(arg: WorkerArg): Int {
    WinApi.setThreadDesktop(WinDesktop(arg.desktop.desktop,
        arg.desktop.name))
    if (arg.program != null) {
        if (arg.desktop.name != null) {
            memScoped {
                val startupInfo = cValue<STARTUPINFOA> {
                    lpDesktop = arg.desktop.name.cstr.ptr
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
        } else {
            MessageBoxA(null,
                "Cannot run the program. Cannot obtain the created desktop's name.",
                "Secure Desktop",
                (MB_OK or MB_ICONERROR).convert())
        }
    }
    return MessageBoxA(null,
        "Hi! This is message from thread ${GetCurrentThreadId()} in the secure desktop." +
                "\nTo load a custom program, run Desktop.exe <path\\to\\your\\program\\.exe>." +
                "\nYou can exit the desktop when you close the dialog or press OK. Programs in the desktop won't be terminated when you exit.",
        "Secure Desktop",
        (MB_OK or MB_ICONINFORMATION).toUInt())
}