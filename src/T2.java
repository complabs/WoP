
/**
 *  The class <code>T2</code> implements <code>Runnable</code> class.
 *  Instance of the <code>T2</code> prints out its signature
 *  until it gets stopped. It also creates a charged particle pair
 *  in associated <code>JPWorld</code> context.
 * 
 *  @author Mikica B Kocic
 */
public class T2 implements Runnable
{
    private String signature = "T2: Thread 2";
    
    /**
     *  Indicates if thread is (or should be) running
     */
    private volatile boolean running = false;
    
    /**
     *  Indicates that thread is active and not paused
     */
    private volatile boolean active  = false;
    
    /**
     *  Instance of the main thread.
     */
    private volatile Thread mainThread = null;
    
    /**
     *  Sleep interval for thread in millis.
     */
    private int sleepIntervalMillis;
    
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
    public T2( JPWorld parent, float sleepIntervalSec )
    {
        this.parent = parent;
        this.sleepIntervalMillis = (int)( sleepIntervalSec * 1000f );
    }

    /**
     *  Starts the thread
     */
    public void startThread ()
    {
        synchronized( this )
        {
            if ( mainThread != null ) {
                return;  // Allow only single thread per instance
            }
            
            running = true;
            active  = true;
            mainThread = new Thread( this );
            mainThread.start ();
        }
    }

    /**
     *  Stops the thread
     */
    public void stopThread ()
    {
        synchronized( this )
        {
            running = false;
            active  = false;
            this.notifyAll ();
        }
    }

    /**
     *  Disables (pauses) the thread
     */
    public void disableThread ()
    {
        synchronized( this )
        {
            active = false;
            this.notifyAll ();
        }
    }

    /**
     *  Enables the thread after paused
     */
    public void enableThread ()
    {
        synchronized( this )
        {
            active = true;
            this.notifyAll ();
        }
    }
    
    /**
     *  Interruptible sleep (replacement for <code>Thread.sleep</code>).
     *  
     *  @param millis - the length of time to sleep in milliseconds. 
     */
    public void interruptibleSleep( long millis )
    {
        synchronized( this )
        {
            try {
                this.wait( millis );
            }
            catch( InterruptedException ie ) {
                running = false; // signals thread to quit
            }
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
            while( running && active )
            {
                parent.println( signature );
                
                parent.createPairOfChargedParticles( 1, 10.0 + Math.random () * 5.0 );
               
                interruptibleSleep( sleepIntervalMillis );
            }
            
            while( running && ! active )
            {
                interruptibleSleep( sleepIntervalMillis );
            }
        }
        
        synchronized( this )
        {
            running    = false;
            active     = false;
            mainThread = null;
        }
    }
}