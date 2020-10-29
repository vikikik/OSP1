package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    private Lock lock;
    private Condition writers;
    private int numWriter = 0;
    private Condition listeners;
    private int numListener = 0;

    private int wordShared;
    private boolean wordUsed = false;
    

    public Communicator() {
        lock = new Lock();
        writers = new Condition(lock);
        listeners = new Condition(lock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        boolean preState = Machine.interrupt().disable();
        lock.acquire();

        while (wordUsed || (numListener==0)) {
            //System.out.println("暂时没有收听者，等待收听");
            numWriter ++;
            writers.sleep();
        }

        this.wordShared = word;
        wordUsed = true;
        listeners.wake();
        numWriter --;

        lock.release();
        Machine.interrupt().restore(preState);
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
        boolean preState = Machine.interrupt().disable();
        lock.acquire();

        while (!wordUsed) {
            //System.out.println("暂时没有说话者，等待说话");
            numListener ++;
            writers.wake();
            listeners.sleep();
        }

        int word = wordShared;
        wordUsed = false;
        numListener --;
        writers.wake(); // write a word

        lock.release();
        Machine.interrupt().restore(preState);
	    return word;
    }

    private static final char dbgCommunicator = 'c';
    public static void selfTest() {
        

		KThread speak, listen;
		
		Lib.debug(dbgCommunicator, "Communicator Self Test");

		// Test that a single word is passed successfully
		speak = new KThread(new TestSpeakerThread(23));
		listen = new KThread(new TestListenerThread());
		
		speak.fork(); listen.fork();
        listen.join();speak.join(); 
        //KThread.yield();
		System.out.println(TestChattyThread.getReceived() == 23);

	
    }
	
	/**
	 * General Communicator test class
	 * Allows subclasses to set the current state of a test and to update 
	 * a running tally of all received words, which are used by selfTest
	 * to verify that communication occurred properly.
	 */
	private abstract static class TestChattyThread implements Runnable {
		static Communicator comm = new Communicator();
		
	
		static int received = 0;

		static void updateReceived(int word) {
			received = word;
		}
		public static int getReceived() {
			return received;
		}
	}
	
	private static class TestSpeakerThread extends TestChattyThread {
		int word;

		TestSpeakerThread(int word) {
			this.word = word;
		}
		
		public void run() {
			comm.speak(word);
		}
	}
	
	private static class TestListenerThread extends TestChattyThread {
		public void run() {
			updateReceived(comm.listen());
		}
	}
}
