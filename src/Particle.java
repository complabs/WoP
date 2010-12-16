
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.RadialGradientPaint;
import java.awt.Transparency;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 *  The <code>Particle</code> encapsulates both particle physical properties and
 *  visual properties needed for rendering during animation.
 *  
 *  Physical behavior of the particle is governed by gravitational and electrical forces
 *  conducted by Newton's law of motion in form of differential equations (applied to 
 *  particle's mass, charge, position, velocity and acceleration) and solved 
 *  (integrated) using numerical Euler's method. Each particle has separate thread
 *  integrating equations thus effectively "moving" particle.
 *  
 *  Forces acting on the particle are set by particle's <code>WorldOfParticle</code> 
 *  that overviews interactions between all particles and calculates summary forces for
 *  each single particles. 
 *  
 *  The resulting 'motion' of the particle is then rendered to 
 *  <code>WorldOfParticle</code>'s <code>Graphics</code> context. Particle image is
 *  generated once and kept in <code>image</code> internal object. Charged particles
 *  are either rendered in Red (positive) or Green (negative). Neutral particles are
 *  rendered in Blue.
 *  
 *  @author Mikica B Kocic
 */
class Particle implements Runnable
{
    private final static double k_e       = 5e6;  // WOP's Coulomb's constant
    private final static double k_G       = 1e-5; // WOP's Gravitational constant
    private final static double k_x       = 1e-4; // WOP's Drag-force constant
    private final static double minRsq    = 91;   // WOP's Quantum-mechanical limit
    private final static double maxVsq    = 4e4;  // Drag-force acts above this limit

    /**
     *  Particle always belong to some world of particles. This is our world.
     */
    private WorldOfParticles world;
    
    /*  Physical properties for the particle that does not change
     *  over the time.
     */
    private double mass   = 1; // Mass
    private double charge = 1; // Electrical charge
    private double radius = 1; // Radius
    
    /*  Physical properties for the particle (position, velocity etc)
     */
    private double xPos   = 0; // Position, x-component
    private double yPos   = 0; // Position, y-component
    private double vx     = 0; // Velocity, x-component
    private double vy     = 0; // Velocity, y-component
    private double vsq    = 0; // Squared velocity 
    private double ax     = 0; // Acceleration, x-component
    private double ay     = 0; // Acceleration, y-component
    private double asq    = 0; // Squared acceleration 
    private double Fx     = 0; // Force, x-component
    private double Fy     = 0; // Force, y-component

    /**
     *  Particle's life time. Must be positive number or positive infinity.
     *  Negative values indicates dead particle. 
     */
    private double lifeTime = Double.POSITIVE_INFINITY;

    /**
     *  Each object (i.e. particle) follows the Newton's laws of motion (differential
     *  equations) that are solved (integrated) in object's (particle's) main thread. 
     */
    private Thread mainThread;
    
    ////////////////////////////////////////////////////////// VISUAL COMPONENTS /////////
    
    /**
     *  Image of the particle that is rendered during animation.
     */
    private BufferedImage image = null;
    
    /**
     *  Particle color.
     */
    private Color color = Color.BLUE; // neutral = blue, positive = red, negative = green
    
    /**
     *  Depth (levels) of anti-aliased edges (anti-aliasing disabled if 0).
     */
    private int aliasedEdges = 10;

    /*  Information for particle's ghost trail (blurred ghost images behind 
     *  moving particle) 
     */
    private boolean showTrail   = false;  // Indicator
    private int     trailSize   = 0;      // Number of ghost images in trail
    private int     trailRatioC = 0;      // Refresh to trail update ratio
    private int     trailRatio  = 0;      // Current ratio value
    private int[]   trailX      = null;   // X-positions for ghost images
    private int[]   trailY      = null;   // Y-positions for ghost images
    private float[] trailFade   = null;   // Fading used for ghost images

    ////////////////////////////////////////////////////////// METHODS ///////////////////
    
    /**
     *  Creates a new instance of Particle.
     * 
     *  @param  parent  the world to which <code>Particle</code> belongs to
     *  @param  M       mass of the <code>Particle</code> 
     *  @param  Q       electrical charge of the <code>Particle</code> 
     *  @param  X       initial position, x-component 
     *  @param  Y       initial position, y-component
     *  @param  R       radius range (0 for random radius) 
     *  @param  T       life time in seconds
     */
    public Particle( WorldOfParticles parent, 
            double M, double Q, double X, double Y, double R, double T
            )
    {
        world    = parent;
        mass     = M;
        charge   = Q;
        lifeTime = T;
        
        /* Initial position
         */
        xPos = X;
        yPos = Y;

        /* Random initial velocity
         */
        vx   = -100 + 200 * Math.random ();
        vy   = -100 + 200 * Math.random ();
        vsq  = Math.pow( vx, 2 ) + Math.pow( vy, 2 );

        /* No initial acceleration
         */
        ax   = 0;
        ay   = 0;
        asq  = 0;

        /* If the radius is not specified (<=0) then randomize radius
         */
        radius = R > 0 ? R : 10.0 + ( 10.0 - R ) * Math.random ();
        
        /* Set color depending on charge: Blue (0), Green (+) Red (-)
         */
        if ( charge == 0 ) {
            color = new Color( 0, 0, 1f );
        } else if ( charge > 0 ) {
            color = new Color( (float)charge, 0, 0 );
        } else {
            color = new Color( 0, -(float)charge, 0 );
        }

        /* Setup ghost trail
         */
        initBlurTrail( 10 );
        
        /* Start moving (solving differential equations for) the particle...
         */
        mainThread = new Thread( this );
        mainThread.start ();
    }

    //////////////////////////////////////////////////////////////////////////////////////
    
    /**
     *  Gets the x-component of particle position
     *  @return the x-component of particle position
     */
    public double getX () { return xPos; }
    
    /**
     *  Gets the y-component of particle position
     *  @return the y-component of particle position
     */
    public double getY () { return yPos; }

    /**
     *  Gets the kinetic energy for the particle
     *  @return the kinetic energy for the particle
     */
    public double getKineticEnergy () 
    {
        return mass * vsq / 2;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////

    /**
     *  Creates blur trail context (faded ghost images for particle) 
     * 
     *  @param size   trail depth (number of ghost images to show)
     */
    private void initBlurTrail( int size )
    {
        showTrail   = true;
        trailSize   = size;
        trailRatioC = 3;
        trailRatio  = 0;
        
        /* Create blur arrays with x,y positions and fading for ghost images
         */
        trailX    = new int  [ trailSize ];
        trailY    = new int  [ trailSize ];
        trailFade = new float[ trailSize ];
        
        float incrementalFactor = .2f / ( trailSize + 1 );
        
        for( int i = 0; i < trailSize; ++i ) 
        {
            /* Default values for positions -1 indicates 
             * not to render these until with real values
             */
            trailX[ i ] = -1;
            trailY[ i ] = -1;
            
            /* The ghost is more faded as it is further away from the particle
             */
            trailFade[i] = ( .2f - incrementalFactor ) - i * incrementalFactor;
        }
    }

    /**
     *  Dumps x/y-components of position, velocity and acceleration to 
     *  <code>System.out</code>.
     */
    public void dump () 
    {
        System.out.printf( "%8.1f %8.1f %8.1f %8.1f %8.1f %8.1f %8.1f\n", 
                           lifeTime, xPos, yPos, vx, vy, ax, ay );
    }

    /**
     *  Resets velocity and acceleration for particle to 0.
     */
    public void resetVelocity () 
    {
        vx = vy = vsq = 0;
    }

    /**
     *  Makes velocity centripetal to acceleration
     */
    public void makeCentripetalVelocity () 
    {
        vx = -ay;
        vy = ax;
    }
    
    /**
     *  Resets total force to 0 ( so it could be summed up again).
     */
    public void resetForce ()
    {
        Fx = 0; Fy = 0.0;
    }

    /**
     *  Calculates particular force that acts on <code>this</code> particle when 
     *  interacting with some other remote particle <code>p</code>. 
     * 
     *  @param   p    remote particle
     */
    public void addForceFromParticle( Particle p )
    {
        if ( p == this ) {
            return;
        }

        /* Rsq = squared distance R between particles
         */
        double Rsq = Math.pow( p.xPos - xPos, 2 ) 
                   + Math.pow( p.yPos - yPos, 2 );

        /* Quantum-mechanics allows distances between particles above some limit 
         * (like Pauli's Exclusion Principle and nuclear forces)
         * This suppresses that we have infinite interaction forces between particles.
         */
        if ( Rsq < minRsq ) {
            Rsq = minRsq;
        }

        /* Radius vector Cartesian coordinate components
         */
        double cos = ( p.xPos - xPos ) / Math.sqrt( Rsq );
        double sin = ( p.yPos - yPos ) / Math.sqrt( Rsq );
        
        /* Coulomb's force between electrical charges
         */
        double e_force = -k_e * charge * p.charge / Rsq;
        Fx += e_force * cos;
        Fy += e_force * sin;

        /* Gravitational force between particle masses
         */
        double g_force = k_G * mass * p.mass / Rsq;
        Fx += g_force * cos;
        Fy += g_force * sin;
    }

    /**
     *  Applies accumulated (summed up) total force on the particle.
     */
    public void applyForce ()
    {
        ax = Fx / mass;
        ay = Fy / mass;
        
        /* Introduce additional drag-force for velocities > sqrt(maxVsq)
         * to slow-down very fast particles.
         */
        if ( vsq > maxVsq ) {
            ax += -k_x * vsq * vx / Math.sqrt( vsq );
            ay += -k_x * vsq * vy / Math.sqrt( vsq );
        }
        
        asq = Math.pow( ax, 2 ) + Math.pow( ay, 2 );
    }

    /**
     *  Moves the particle (overriding forces) and resets its velocity.
     *  
     *  @param dx   delta x-position
     *  @param dy   delta y-position
     */
    public void moveParticle( double dx, double dy )
    {
        xPos += dx; yPos += dy;
        resetVelocity ();
    }

    /**
     *  Keeps the particle inside infinite potential barrier.
     * 
     *  @param barrier boundaries of infinite potential barrier given as array 
     *                 {x1,y1,x2,y2} of coordinates
     */
    public void forceBarrier( WorldOfParticles.Barrier barrier )
    {
        final double k = 0.5; // in-elastic collision coefficient 
        
        if ( xPos < barrier.xBeg ) {
            xPos = barrier.xBeg; 
            vx = -k * vx;
        } else if ( xPos > barrier.xEnd ) {
            xPos = barrier.xEnd; 
            vx = -k * vx ;
        }

        if ( yPos < barrier.yBeg ) {
            yPos = barrier.yBeg; 
            vy = -k * vy;
        } else if ( yPos > barrier.yEnd ) {
            yPos = barrier.yEnd; 
            vy = -k * vy;
        }
    }

    /**
     *  Moves the particle using numerical integration of differential equations
     *  of the motion by Euler's method.
     * 
     *  @param dT  integration interval in seconds
     */
    private void integrateNewtonLaws( double dT )
    {
        synchronized( world )
        {
            xPos += vx * dT;
            yPos += vy * dT;

            vx   += ax * dT;
            vy   += ay * dT;

            vsq = Math.pow( vx, 2 ) + Math.pow( vy, 2 );
            
            forceBarrier( world.getBarrier () );
        }
    }
    
    /**
     *  Draws the circle with radial gradient from the center
     */
    private void drawGradCircle( Graphics2D g2d,
            float x, float y, float radius, Color color
            )
    {
        Point2D center = new Point2D.Float( x, y );
        
        float[] dist = { 0.0f, 1.0f };
        
        float[] c = { color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f };
        Color[] colors = {
                new Color( c[0] * 1.0f, c[1] * 1.0f, c[2] * 1.0f, 1.0f ), // base color
                new Color( c[0] * 0.8f, c[1] * 0.8f, c[2] * 0.8f, 0.0f )  // fade out
                };
        
        g2d.setPaint( 
                new RadialGradientPaint( 
                        center, radius, dist, colors, CycleMethod.NO_CYCLE
                        )
                );
        
        g2d.fill( new Ellipse2D.Float( x - radius, y - radius, 2*radius, 2*radius ) );
    }

    /**
     *  Creates the image of the particle that will be animated.
     */
    private void createParticleImage () 
    {
        GraphicsConfiguration gc = world.getGraphicsConfiguration ();
        if ( gc == null ) {
            return;
        }

        int diameter = (int)( radius * 2 );
        
        image = gc.createCompatibleImage( diameter, diameter, Transparency.TRANSLUCENT );

        Graphics2D gImg = image.createGraphics ();
        if ( aliasedEdges > 0 )
        {
            drawGradCircle(gImg, (int)radius, (int)radius, (int)radius, color ); 
        }
        else
        {
            gImg.setColor( color );
            gImg.fillOval( 0, 0, diameter, diameter );
        }

        gImg.dispose ();
    }
    
    /**
     *  Renders the particle in Graphics context.
     *  
     *  @param  gr          the Graphics context in which particle is rendered
     *  @param  annotate    indicator whether additional info about particle's 
     *                      acceleration is displayed or not
     */
    public void paint( Graphics gr, boolean annotate ) 
    {
        if ( image == null ) {
            createParticleImage();
            if ( image == null ) {
                return;
            }
        }

        Graphics2D g = (Graphics2D)gr.create ();

        /* Convert (px,py) at the center of the particle to (x,y) as base of the image
         */
        int px = (int)xPos;
        int py = (int)yPos;
        int x = (int)( xPos - radius );
        int y = (int)( yPos - radius );

        /* Base fade for the particle is derived its life time (fades out at the end).
         */
        float baseFade = (float)( lifeTime < 0 ? 0 : lifeTime > 1 ? 1 : lifeTime );
        
        if ( showTrail ) {

            /* Draw previous locations of the particle as a trail of ghost images
             */
            for( int i = 0; i < trailSize; ++i ) {
                if( trailX[i] >= 0 ) {
                    /* Render each particle ghost with fading */
                    g.setComposite( 
                            AlphaComposite.SrcOver.derive( baseFade * trailFade[i] )
                            );
                    g.drawImage( image, trailX[i], trailY[i], null );
                }
            }

            --trailRatio;

            if ( trailRatio <= 0 ) {
                
                trailRatio = trailRatioC;

                /* Shift the ghost trail positions in array (from newest to eldest)
                 */
                for( int i = trailSize - 1; i > 0; --i ) {
                    trailX[ i ] = trailX[ i - 1 ];
                    trailY[ i ] = trailY[ i - 1 ];
                }
                trailX[ 0 ] = x;
                trailY[ 0 ] = y;
            }
        }

        g.setComposite( AlphaComposite.SrcOver.derive( baseFade ) );
        
        if ( annotate && asq > 0.0 )
        {
            /* Vector pointing in force direction with length proportional to 
             * the logarithm of acceleration (with small linear fix for dramatic purpose)
             */
            int dx = asq < 1e-9 ? 0 
                : (int)( 1e-3 * ax + 3 * ax * Math.log( 1 + asq ) / Math.sqrt( asq ) );
            int dy = asq < 1e-9 ? 0 
                : (int)( 1e-3 * ay + 3 * ay * Math.log( 1 + asq ) / Math.sqrt( asq ) );
            
            Color cText = new Color( color.getRGB () ).darker ();
            
            /* Draw acceleration vector
             */
            g.setColor( cText.darker () );
            g.drawLine( px, py, px + dx, py + dy );
            g.drawArc( px + dx - 4, py + dy - 4, 8, 8, 0, 360 );
            
            /* Show acceleration values
             */
            g.setColor( cText );
            Font f = new Font( Font.MONOSPACED, Font.PLAIN, 14 ); // TODO: static?
            g.setFont( f );
            g.drawString( String.format( "%+7.1f", ax ), px + dx - 4, py + dy - 4 - 14 );
            g.drawString( String.format( "%+7.1f", ay ), px + dx - 4, py + dy - 4 );
        }
        
        g.drawImage( image, x, y, null );
        
        if ( annotate && true )
        {
            g.setColor( Color.BLACK );
            int R = (int)( radius / 3 );
            if ( charge != 0 ) {
                g.drawLine( px - R, py, px + R, py );
            }
            if ( charge > 0 ) {
                g.drawLine( px, py - R, px, py + R );
            }
        }

        g.dispose ();
    }

    /**
     *  Kills the particle by setting particle's life time to 0.
     *  Particle is dead (moving thread exits) if its life time < 0.
     */
    public void kill ()
    {
        lifeTime = 0.0;
        if ( mainThread != null ) {
            mainThread.interrupt ();
        }
    }

    /**
     *  Thread.sleep wrapper.
     */
    public void interruptibleSleep( long millis )
    {
        synchronized( this )
        {
            try {
                Thread.sleep( 10 );
            }
            catch( InterruptedException ie ) {
                lifeTime = 0; // kill thread
            }
        }
    }

    /**
     *  Main thread that solves Newton's laws of motion, effectively moving
     *  the particle in the parent <code>WorldOfParticles</code> (unless the world 
     *  is paused i.e. freezed). Thread also keeps particle's life time and after
     *  it expires, particle is removed from the belonging world.
     */
    @Override
    public void run () 
    {
        final int sleepMillis = 5; // ~200 Hz
        
        long oldTime = System.nanoTime ();
            
        while( lifeTime >= 0 ) // particle is dead when its life time becomes lt. 0
        {
            interruptibleSleep( sleepMillis );
            long currentTime = System.nanoTime ();
            
            /* Calculates sleep time dT in seconds. dT is not necessarily the same as
             * the intentioned sleep time. It can be scaled further to speed up or 
             * slow-down particle's motion.
             */
            double dT = world.getTimeScale () * (double)( currentTime - oldTime ) / 1e9;

            if ( ! world.isPaused () ) {
                integrateNewtonLaws( dT );
            }

            lifeTime -= dT;
            oldTime = currentTime;
        }
        
        /* Cleans-up dead particle
         */
        world.removeParticle( this );
    }
}
