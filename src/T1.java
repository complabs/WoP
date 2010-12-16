
/**
 *  The class <code>T1</code> extends <code>Thread</code> class.
 *  Instance of the <code>T2</code> prints out its signature
 *  until it gets stopped. It also creates a charged particle pair
 *  in associated <code>JPWorld</code> context.
 * 
 *  @author Mikica B Kocic
 */
public class T1 extends Thread 
{
    private String signature = "T1: Thread 1";
    
    /**
     *  Indicates if thread is (or should be) running
     */
    private volatile boolean running = false;
    
    /**
     *  Indicates that thread is completed
     */
    private volatile boolean completed = true;

    /**
     *  Sleep interval for thread in millis.
     */
    private int sleepItervalMillis;

    /**
     *  Parent where println() messages are directed to.
     */
    protected JPWorld parent;

    /**
     *  Creates a new instance of T1.
     * 
     *  @param parent            owner of the thread
     *  @param sleepIntervalSec  sleep interval in seconds
     */
    public T1( JPWorld parent, float sleepIntervalSec )
    {
        this.parent = parent;
        this.sleepItervalMillis = (int)( sleepIntervalSec * 1000f );
    }

    /**
     *  Starts the thread.
     */
    @Override
    public void start ()
    {
        if ( ! completed ) {
            return;  // Allow only single thread per instance
        }

        running = true;
        completed = false;
        super.start ();
    }

    /**
     *  Stops the thread.
     */
    public void stopThread ()
    {
        running = false;
        this.interrupt (); // interrupt ongoing sleep
    }
    
    /**
     *  Interruptible sleep (instead of <code>Thread.sleep</code>).
     *  
     *  @param millis - the length of time to sleep in milliseconds. 
     */
    public void interruptibleSleep( long millis )
    {
        try {
            Thread.sleep( millis );
        }
        catch( InterruptedException ie ) {
            /* ignored */
        }
    }

    /**
     *  Prints thread's own signature and creates a pair of particles in belonging
     *  <code>JPWorld</code>'s context every <code>sleepItervalMillis</code>. 
     */
    @Override
    public void run ()
    {
        while( running )
        {
            parent.println( signature );
            
            parent.createPairOfChargedParticles( 1, 10.0 + Math.random () * 5.0 );

            interruptibleSleep( sleepItervalMillis );
        }
        
        completed = true;
    }
}
