
/**
 *  The class <code>T3</code> extends <code>T1</code> class.
 *  It is a test wrapper for <code>T1</code> and <code>T2</code> classes
 *  according to the following specification:
 *  <pre>
 *   1. Create and start thread from class T1
 *   2. Wait 5 seconds
 *   3. Create and start thread from class T2
 *   4. Wait 5 seconds
 *   5. Pause thread from class T2
 *   6. Wait 5 seconds
 *   7. Activate thread from class T2
 *   8. Wait 5 seconds
 *   9. Kill thread from class T1
 *  10. Wait 5 seconds
 *  11. Kill thread from class T2
 *  </pre>
 *
 *  @author Mikica B Kocic
 */
public class T3 extends T1 
{
    private String signature = "T3: Thread 3";
    
    private float initialDelaySec = 0f;
    
    /**
     * Creates a new instance of T3.
     */
    public T3( JPWorld component, float sleepIntervalSec, float initialDelaySec )
    {
        super( component, sleepIntervalSec );
        this.initialDelaySec = initialDelaySec;
    }

    /**
     *  Test procedure for T1 and T2 classes running in separate thread.
     */
    @Override
    public void run ()
    {
        interruptibleSleep( (int)( initialDelaySec * 1000f ) );
        
        parent.clearLogArea (); // clear log area first

        parent.println( signature + ": Running..." );

        interruptibleSleep( 1000 );
        
        parent.println( "------ Creating T1" );
        T1 t1 = new T1( parent, 1.0f );

        parent.println( "------ Starting T1" );
        t1.start ();

        interruptibleSleep( 5000 );
        
        parent.println( "------ Creating T2" );
        T2 t2 = new T2( parent, 1.0f );

        parent.println( "------ Starting T2" );
        t2.startThread ();
        
        interruptibleSleep( 5000 );
        
        parent.println( "------ Disabling T2" );
        t2.disableThread ();
        
        interruptibleSleep( 5000 );
        
        parent.println( "------ Enabling T2" );
        t2.enableThread ();
        
        interruptibleSleep( 5000 );
        
        parent.println( "------ Killing T1" );
        t1.stopThread ();
        
        interruptibleSleep( 5000 );
        
        parent.println( "------ Killing T2" );
        t2.stopThread ();
        
        parent.println( signature + ": Done." );
        parent.afterTestsCompleted ();
    }
}
