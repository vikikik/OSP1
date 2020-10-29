package nachos.threads;

import java.util.ArrayList;
import java.util.Iterator;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    private class alarmAndSleeper {
        private KThread sleeper;
        private long wakeTime;

        public alarmAndSleeper(KThread s, long t) {
            this.sleeper = s;
            this.wakeTime = t;
        }
        public KThread getThread() {
            return this.sleeper;
        }
        public long getWakeTime() {
            return this.wakeTime;
        }
    }

    private ArrayList<alarmAndSleeper> sleeperList = new ArrayList<alarmAndSleeper>();

    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        boolean preState = Machine.interrupt().disable();
        
        alarmAndSleeper a;
        for (java.util.Iterator i = sleeperList.iterator(); i.hasNext();){
            a = (alarmAndSleeper)i.next();
            if (a.getWakeTime() <= Machine.timer().getTime()) {
                i.remove();
                a.getThread().ready();
            }
        }

        Machine.interrupt().restore(preState);//恢复中断
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
        // for now, cheat just to get something working (busy waiting is bad)
        if (x <= 0) {
            timerInterrupt();
            return;
        }

        boolean preState = Machine.interrupt().disable();

	    long wakeTime = Machine.timer().getTime() + x;
	    /*while (wakeTime > Machine.timer().getTime())
            KThread.yield();*/
        KThread c = KThread.currentThread();
        sleeperList.add(new alarmAndSleeper(c, wakeTime));

        System.out.println(sleeperList.size());
        c.sleep();

        Machine.interrupt().restore(preState);
    }

    public static void selfTest(){
        System.out.println("Alarm selfTest");
        KThread a = new KThread(new Runnable() {
            public void run() {
                System.out.println("Thread 1 sleeps, time: "+Machine.timer().getTime());
                ThreadedKernel.alarm.waitUntil(800);
                System.out.println("Thread 1 wakes, time:"+Machine.timer().getTime());
                KThread.currentThread().yield();
            }
        }
        );
        KThread b = new KThread(new Runnable() {
            public void run() {
                System.out.println("Thread 2 sleeps, time: "+Machine.timer().getTime());
                ThreadedKernel.alarm.waitUntil(500);
                System.out.println("Thread 2 wakes, time:"+Machine.timer().getTime());
                KThread.currentThread().yield();
            }
        }
        );
        a.fork(); b.fork();
        a.join(); b.join();
    }
}
