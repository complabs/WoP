
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.util.ArrayList;

/**
 *  The <code>WorldOfParticles</code> is container for particles governed by 
 *  the common physical laws. Forces acting on particles belonging to this world
 *  are calculated in the main thread of the instance. The instance of the
 *  <code>WorldOfParticles</code> is rendered inside the <code>Component</code>, which
 *  acts as a parent.
 *   
 *  @author Mikica B Kocic
 */
public class WorldOfParticles implements Runnable 
{
    /**
     *  Provides rendering context interface for the <code>WorldOfParticles</code>.
     */
    public interface RenderingContext
    {
        public abstract void onParticleCountChanged( int newParticleCount );
        public abstract Barrier getBarrier ();
        public abstract GraphicsConfiguration getGraphicsConfiguration ();
    }

    //////////////////////////////////////////////////////////////////////////////////////

    /**
     *  Infinite Potential Barrier (2D-Box) class
     */
    protected class Barrier
    {
        public double xBeg;
        public double yBeg;
        public double xEnd;
        public double yEnd;
        
        public Barrier( double xBeg, double yBeg, double xEnd, double yEnd )
        {
            this.xBeg = xBeg; this.yBeg = yBeg;
            this.xEnd = xEnd; this.yEnd = yEnd;
        }
    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    
    /**
     *  Collection of <code>Particles</code> that belongs to this world.
     */
    private ArrayList<Particle> particles = new ArrayList<Particle> ();

    /**
     *  World's time to 'real' world's time ratio. Slower if less then 1.
     */
    private volatile double timeScale = 0.5;

    /**
     *  Indicates whether forces between particles are calculated or not.
     *  The velocities of the particles are freezed if forces are not calculated.
     */
    private volatile boolean paused = false;
    
    /**
     *  Indicates whether to keep world's thread running or not.
     */
    private volatile boolean running = true;
    
    /**
     *  Each object (i.e. world) has it's own physical rules. Forces between particles 
     *  in our world are are calculated in object's main thread. 
     */
    private Thread mainThread;

    /**
     *  Particular instance with rendering context interface
     */
    private RenderingContext context;

    //////////////////////////////////////////////////////////////////////////////////////

    /**
     *  Creates and starts object's (world's) main thread.
     * 
     *  @param context  instance of the rendering context 
     */
    public WorldOfParticles( RenderingContext context ) 
    {
        this.context = context;

        mainThread = new Thread( this );
        mainThread.start ();
    }

    /**
     *  Returns indicator whether forces between particles are calculated or not.
     */
    public boolean isPaused ()
    {
        return paused;
    }
    
    /**
     *  Turns on/off calculations of forces between particles.
     */
    public void togglePaused ()
    { 
        paused = ! paused; 
    }

    /**
     *  Gets number of particles in our world.
     * 
     *  @return number of particles
     */
    public int getParticleCount ()
    {
        synchronized( this )
        {
            return particles.size ();
        }
    }

    /**
     *  Increments/decrements time scale
     *  
     *  @param factor  delta time
     */
    public void incTimeScale( double factor )
    {
        timeScale += factor;
        timeScale = Math.min( timeScale, 2.0 );
        timeScale = Math.max( timeScale, 0.1 );
    }

    /**
     *  Gets world's time to 'real' world's time ratio. 
     *  
     *  @return time scale; slower if less then 1.
     */
    public double getTimeScale ()
    {
        return timeScale;
    }

    /**
     *  Gets the GraphicsConfiguration associated with parent Component.
     * 
     * @return the GraphicsConfiguration used by parent's Component or null
     */
    public GraphicsConfiguration getGraphicsConfiguration()
    {
        return context.getGraphicsConfiguration ();
    }

    /** Adds one extra particle to the world.
     * 
     *  @param  M       mass of the <code>Particle</code> 
     *  @param  Q       electrical charge of the <code>Particle</code> 
     *  @param  X       initial position, x-component 
     *  @param  Y       initial position, y-component
     *  @param  R       radius range
     *  @param  T       life-time in seconds
     */
    public Particle addNewParticle( 
            double M, double Q, double X, double Y, double R, double T ) 
    {
        Particle p = null;
        
        synchronized( this )
        {
            p = new Particle ( this, M, Q, X, Y, R, T );
            
            particles.add ( p );
            
            // Inform parent that particle count has changed
            //
            context.onParticleCountChanged( particles.size () );
        }
        
        return p;
    }

    /** Removes the particle <code>p</code> from the world.
     * 
     *  @param  p    instance of the <code>Particle</code> to be removed 
     */
    public void removeParticle( Particle p ) 
    {
        synchronized( this )
        {
            particles.remove( p );
            
            /* Inform parent that number of particles has been changed
             */
            context.onParticleCountChanged( particles.size () );
        }
    }

    /** Dumps internal state for all particles to System.out (for debugging purposes).
     */
    public void dump ()
    {
        synchronized( this )
        {
            System.out.printf( "%8s %8s %8s %8s %8s %8s %8s\n", 
                    "lifetime", "xPos", "yPos", "vx", "vy", "ax", "ay" );

            for( Particle p : particles ) {
               p.dump ();
            }
            
            System.out.printf( "Total %d particles\n", getParticleCount () );
        }
    }

    /**
     *  Resets velocities for all particles to 0.
     */
    public void resetVelocities ()
    {
        synchronized( this )
        {
            for( Particle p : particles ) {
                p.resetVelocity ();
             }
        }
    }

    /**
     *  Makes velocities for all particles to be centripetal to current
     *  particles' acceleration.
     */
    public void makeCentripetalVelocities ()
    {
        synchronized( this )
        {
            for( Particle p : particles ) {
                p.makeCentripetalVelocity ();
             }
        }
    }

    /**
     *  Renders the world on <code>Graphics</code>.
     * 
     *  @param g         where to render
     *  @param annotate  annotate particle accelerations
     */
    public void paint( Graphics g, boolean annotate )
    {
        synchronized( this )
        {
            for( Particle p : particles ) {
                p.paint( g, annotate );
             }
        }
    }

    /**
     *  Gets boundaries of infinite potential barrier keeping particles together.
     */
    public Barrier getBarrier ()
    {
        return context.getBarrier ();
    }

    /**
     *  Calculates forces acting on particles.
     */
    public void calculateInteractions ()
    {
        synchronized( this )
        {
            for ( Particle p : particles ) 
            {
                /* The sum of all forces acting on particle p
                 */
                p.resetForce ();
                
                /* Get sum of forces of interactions with other particles in the world
                 */
                for ( Particle q : particles ) 
                {
                    p.addForceFromParticle( q );
                }

                /* Apply summed forces 
                 */
                p.applyForce ();
                
                /* Relax CPU usage
                 */
                Thread.yield ();                
            }
            
        }
    }

    /**
     *  Main thread that calculates interactions (forces) between all particles
     *  in the world
     */
    @Override
    public void run () 
    {
        final int sleepMillis = 5; // ~200 Hz
        
        while( running )
        {
            try { 
                Thread.sleep( sleepMillis );
            }
            catch( InterruptedException ie ) {
            }

            if ( ! paused ) {
                calculateInteractions ();
            }
        }
    }
}
