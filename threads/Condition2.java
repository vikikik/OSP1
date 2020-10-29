package nachos.threads;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	    this.conditionLock = conditionLock;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        boolean intStatus = Machine.interrupt().disable();

        conditionLock.release();
        
        KThread c = KThread.currentThread();
        waitQueue.waitForAccess(c);
        c.sleep();

        conditionLock.acquire();
        
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        boolean intStatus = Machine.interrupt().disable();

        KThread t = waitQueue.nextThread();
        if (t != null) {
            t.ready();
        }

        Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	    Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        boolean intStatus = Machine.interrupt().disable();

        KThread t = waitQueue.nextThread();
        while (t != null) {
            t.ready();
            t = waitQueue.nextThread();
        }

        Machine.interrupt().restore(intStatus);
    }

    public static void selfTest() {
		/*
		 * The only way we can test this without crashing the program is with a normal sort of test with KThread sleeping on this (just to test basic functionality)
		 */
		System.out.println("Condition2 Self Test");
        KThread thread;
		
		// Verify that thread reacquires lock when woken
		final Lock lock2 = new Lock();
        final Condition2 cond = new Condition2(lock2);
        final Lock lock = new Lock();
        final Condition condT = new Condition(lock);
		
		KThread thread1 = new KThread(new Runnable() {
			public void run() {
				lock2.acquire();
				cond.sleep();
				
				// When I wake up, I should hold the lock
				boolean m = lock2.isHeldByCurrentThread();
                lock2.release();
                
                lock.acquire();
                condT.sleep();
                System.out.println(lock.isHeldByCurrentThread()==m ? "[PASS]" : "[FAIL]");
                
                lock.release();
			}
		});
		
	}
	
	/**
	 * Test class which increments a static counter when woken
	 */
	static class WakeCounter implements Runnable {
		public static int wakeups = 0;
		public static Lock lock = null;
		public static Condition2 cond = null;
		
		public void run() {
			lock.acquire();
			cond.sleep();
			wakeups++;
			lock.release();
		}
	}

    private Lock conditionLock;
    private ThreadQueue waitQueue = ThreadedKernel.scheduler.newThreadQueue(true);
}
