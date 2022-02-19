package work.tinax.jnatimer;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class Main {
    public interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary) Native.load("Kernel32.dll", CLibrary.class);

        interface WaitOrTimerCallback extends Callback {
            void invoke(Pointer lpParameter, boolean timerOrWaitFired);
        }

        Pointer CreateEventA(Pointer lpEventAttributes, boolean bManualReset, boolean bInitialState, Pointer lpName);
        Pointer CreateTimerQueue();
        boolean CreateTimerQueueTimer(PointerByReference phNewTimer, Pointer timerQueue, WaitOrTimerCallback callback, Pointer parameter, int dueTime, int period, NativeLong flags);
        //boolean RegisterWaitForSingleObject(Pointer phNewWaitObject, Pointer hObject, WaitOrTimerCallback callback, Pointer context, NativeLong dwMilliseconds, NativeLong dwFlags);
        int WaitForSingleObject(Pointer hHandle, int dwMilliseconds);
        boolean SetEvent(Pointer hEvent);
        boolean DeleteTimerQueueEx(Pointer timerQueue, Pointer completionEvent);
        boolean CloseHandle(Pointer hObject);
        int GetLastError();
    }

    static Pointer gDoneEvent;

    static class TimerRoutine implements CLibrary.WaitOrTimerCallback {

        @Override
        public void invoke(Pointer lpParam, boolean timerOrWaitFired) {
            if (lpParam == Pointer.NULL) {
                System.out.println("TimerRoutine lpParam is NULL");
            } else {
                var param = lpParam.getInt(0);
                System.out.println("Timer routine called. Parameter is " + param);
                //System.out.println("Timer routine called. Param is located at " + lpParam);
                if (timerOrWaitFired) {
                    System.out.println("The wait timed out.");
                } else {
                    System.out.println("The wait event was signaled.");
                }
            }

            CLibrary.INSTANCE.SetEvent(gDoneEvent);
        }
    }

    public static void main(String[] args) {
        Pointer hTimer = Pointer.NULL;
        Pointer hTimerQueue = Pointer.NULL;
        int arg = 123;

        gDoneEvent = CLibrary.INSTANCE.CreateEventA(Pointer.NULL, true, false, Pointer.NULL);
        if (Pointer.NULL == gDoneEvent) {
            System.out.println("CreateEvent failed " + CLibrary.INSTANCE.GetLastError());
            System.exit(1);
        }

        hTimerQueue = CLibrary.INSTANCE.CreateTimerQueue();
        if (Pointer.NULL == hTimerQueue) {
            System.out.println("CreateTimerQueue failed " + CLibrary.INSTANCE.GetLastError());
            System.exit(2);
        }

        PointerByReference hTimerP = new PointerByReference();
        IntByReference argP = new IntByReference(arg);
        if (!CLibrary.INSTANCE.CreateTimerQueueTimer(
                hTimerP,
                hTimerQueue,
                new TimerRoutine(),
                argP.getPointer(),
                3000,
                0,
                new NativeLong(0))) {
            System.out.println("CreateTimerQueueTimer failed " + CLibrary.INSTANCE.GetLastError());
            System.exit(3);
        }

        System.out.println("Call timer routine in 3 seconds...");

        if (CLibrary.INSTANCE.WaitForSingleObject(gDoneEvent, -1) != 0) {
            System.out.println("WaitForSingleObject failed " + CLibrary.INSTANCE.GetLastError());
        }

        CLibrary.INSTANCE.CloseHandle(gDoneEvent);

        if (!CLibrary.INSTANCE.DeleteTimerQueueEx(hTimerQueue, Pointer.NULL)) {
            System.out.println("DeleteTimerQueue failed " + CLibrary.INSTANCE.GetLastError());
        }

        System.exit(0);
    }
}
