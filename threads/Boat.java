package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
	static BoatGrader bg;
	private static int[] numChild = new int[2];
	private static int[] numAdult = new int[2];
	private static Lock lock = new Lock();
	//private static Condition[] condChild = new Condition[2];
	private static Condition condAdult = new Condition(lock);
	private static Condition condChild = new Condition(lock);
	private static int boatPlace = 0;
	private static boolean pilotKey = true;
	private static int numOnBoat = 0;

	public static void selfTest() {
		BoatGrader b = new BoatGrader();

		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);

		b = new BoatGrader();
		System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		begin(1, 2, b);

		b = new BoatGrader();
		System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		begin(3, 3, b);
	}

	public static void begin(int adults, int children, BoatGrader b) {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here

		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.
		//condChild[0] = new Condition(lock);
		//condChild[1] = new Condition(lock);

		Runnable childThread = new Runnable() {
			public void run() {
				ChildItinerary();
			}
		};
		Runnable adultThread = new Runnable() {
			public void run() {
				AdultItinerary();
			}
		};
		

		KThread[] at = new KThread[adults];
		KThread[] ct = new KThread[children];

		for (int i=0; i<adults; i++) {
			at[i] = new KThread(adultThread);
			at[i].fork();
		}
		for (int i=0; i<children; i++) {
			ct[i] = new KThread(childThread);
			ct[i].fork();
		}
		numChild[0] = children;
		numAdult[0] = adults;

		numOnBoat = 0;
		boatPlace = 0;
		
		for (int i=0; i<adults; i++)
			at[i].join();
		for (int i=0; i<children; i++)
			ct[i].join();
		
		//while (numChild[0]>0 || numAdult[0]>0)
		//	KThread.yield();
		
	}

	static void AdultItinerary() {
		bg.initializeAdult(); // Required for autograder interface. Must be the first thing called.
		// DO NOT PUT ANYTHING ABOVE THIS LINE.

		/*
		 * This is where you should put your solutions. Make calls to the BoatGrader to
		 * show that it is synchronized. For example: bg.AdultRowToMolokai(); indicates
		 * that an adult has rowed the boat across to Molokai
		 */
		lock.acquire();

		if (boatPlace==1 || numChild[0]>=1) {
			condChild.wake();
			condAdult.sleep();
		}
		boatPlace = 1;
		numAdult[0] --;
		numAdult[1] ++;
		bg.AdultRowToMolokai();
		condChild.wake();

		System.out.println("Adult end");

		lock.release();
    }

    static void ChildItinerary()
    {
		bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
		//DO NOT PUT ANYTHING ABOVE THIS LINE. 

		lock.acquire();

		int place = 0;

		while (numAdult[0]>0 || numChild[0]>0) {
			if (place==0) {
				if (boatPlace==1) {
					condChild.wake();
					condChild.sleep();
				}
				boolean isPilot = pilotKey;
				pilotKey = false;

				//System.out.println(numOnBoat);
				//System.out.println(place);

				if (numOnBoat == 0) {
					bg.ChildRowToMolokai();
					numChild[0] --;
					numChild[1] ++;
					numOnBoat ++;
					place = 1;
				}
				else if (numOnBoat == 1) {
					bg.ChildRideToMolokai();
					numChild[0] --;
					numChild[1] ++;
					boatPlace = 1;
					numOnBoat ++;
					place = 1;
				} else {
					condChild.wake();
					condChild.sleep();
				}
				condChild.wake();

				if (numAdult[0]<=0 && numChild[0]<=0) {
					wakeAll();
					break;
				}
			} else if (place==1) {
				if (boatPlace==0) {
					condChild.wake();
					condChild.sleep();
					continue;
				}
				if (numChild[0]>0 || numAdult[0]>0) {
					if (numChild[0]>0)
						condChild.wake();
					else condAdult.wake();
					bg.ChildRowToOahu();
					boatPlace = 0;
					pilotKey = true;
					place = 0;
					numOnBoat = 0;
					numChild[0] ++;
					numChild[1] --;
				}
				if (numAdult[0]<=0 && numChild[0]<=0) {
					wakeAll();
					break;
				}
			}
			condChild.sleep();
		}
		System.out.println("Children end");

		lock.release();
	}
	
	private static void wakeAll() {
		condAdult.wakeAll();
		condChild.wakeAll();
	}

    	static void SampleItinerary()
    {
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		/*System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();*/
    }
    
}
