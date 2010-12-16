
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *  The <code>JPWorld</code> class encapsulates artificial world of charged and neutral
 *  particles governed by physical laws, i.e. electrical and gravitational forces as well
 *  Newton's law of motions. The motion of each particle is calculated in a separate
 *  thread belonging to instance of <code>Particle</code> class. The interacting forces
 *  between particles are calculated in single thread for several <code>Particle</code>s
 *  belonging to the same <code>WorldOfParticles</code>. Instance of the 
 *  <code>WorldOfParticles</code> is rendered (and animated) inside (as a part of) 
 *  the <code>JPWorld</code>.
 *  
 *  Application's short-cut key are summarized in <code>printUsage()</code>.
 *   
 *  @author  Mikica B Kocic
 *
 */
public class JPWorld extends JComponent 
     implements ActionListener, KeyListener, WorldOfParticles.RenderingContext 
{
    /**
     *  Implements java.io.Serializable interface
     */
    private static final long serialVersionUID = 1274716824806299281L;

    /**
     *  The instance of the world of particles that we are rendering. 
     */
    private WorldOfParticles world = null;

    /**
     *  Special particle with very huge mass, also called The Big Blue.
     *  It can be moved by the user. 
     */
    private Particle hugeMass = null;

    /**
     *  Creation of new particles <i>should</i> try to keep total amount of particles in 
     *  the world bellow this limit. 
     */
    private final int maxParticlesAllowed = 100;
    
    /**
     *  Animation timer used to trigger rendering 
     */
    private Timer timer = null;

    /**
     *  Base animation timer resolution 
     */
    private int animationTimerResolution = 5;

    /**
     *  The background color shade of gray
     */
    private int backgroundColor = 255;

    /**
     *  Indicates whether to annotate forces acting on particles or not. 
     */
    private volatile boolean annotateForces = true;

    /**
     *  Log pane containing log area
     */
    private JScrollPane logPane = null;
    
    /**
     *  Log (debug, info, error...) messages (instead of System.out).
     */
    private JTextArea logf = null;

    /**
     *  Instance of the T3 class (used to test T1 & T2 classes) 
     */
    private T3 testerThread = null;

    /**
     *  Creates a new instance of Particle thread tester 
     */
    public JPWorld ()
    {
        this.setLayout( new BorderLayout () );

        /* Create log area used to display textual information to user
         */
        createLogArea ();

        /* Create world of particles...
         */
        world = new WorldOfParticles( this );

        /* Start animation timer...
         */
        startAnimation( animationTimerResolution );
    }

    /**
     *  Shows short-cut keys and application usage in log area.
     */
    public void printUsage ()
    {
        synchronized( this )
        {
            /* Do not print usage while testerThread is running
             */
            if ( testerThread != null ) {
                return;
            }

            clearLogArea ();
            
            boolean hasFrame = ( SwingUtilities.getRoot(this) instanceof RunStandalone );
            
            /* Now, show short-cut keys...
             */
            println( "" );
            println( "        Usage:" );
            println( "------- ----------------------" );
            println( "   A    create one +/- pair" );
            println( "   B    create 5 pairs of +/-" );
            println( "   C    create 50 pairs of +/-" );
            println( "   E    centripetalize vel." );
            
            if ( hasFrame ) {
                println( "   F    toggle fullscreen" );
            }

            println( "   H    help (usage)" );
            println( "   N    annotate forces" );
            println( "   O    slow-down time" );
            println( "   P    speed-up time" );
            println( "   R    reset all velocities" );
            println( "   T    start tests" );
            println( "   +    inc. frame interval" );
            println( "   -    dec. frame interval" );
            println( " space  freeze/unfreeze" );
            println( " arrows move The Big Blue" );
            
            if ( hasFrame ) {
                println( "  esc   exit fullscreen" );
            }
            
            println( "" );
        }
    }

    /**
     *  Indicates if in fullscreen mode
     */
    private boolean isFullScreen ()
    {
        if ( ! ( SwingUtilities.getRoot( this ) instanceof RunStandalone ) ) {
            return false;
        }
        
        RunStandalone jf = (RunStandalone)( SwingUtilities.getRoot( this ) );
        
        return jf.isUndecorated ();
    }
    
    /**
     *  Exits full-screen mode
     */
    private void exitFullScreen ()
    {
        if ( isFullScreen () ) {
            toggleFullScreen ();
        }
    }
    
    /**
     *  Toggles full-screen mode
     */
    private void toggleFullScreen ()
    {
        if ( ! ( SwingUtilities.getRoot( this ) instanceof RunStandalone ) ) {
            return; // not applicable to Applet mode
        }
        
        RunStandalone jf = (RunStandalone)( SwingUtilities.getRoot( this ) );
        
        /* suppress repaints and notifications
         */
        jf.setVisible( false );
        jf.setIgnoreRepaint( true );
        jf.removeNotify ();
        
        /* toggle window title and border
         */
        jf.setUndecorated( ! jf.isUndecorated () );

        /* enable repaints and notifications, then show visible
         */
        jf.addNotify ();
        jf.setIgnoreRepaint( false );
        
        /* rearange components and windows position
         */
        if ( jf.isUndecorated () )
        {
            logPane.setVisible( false );
            
            /* maximize window
             */
            jf.setExtendedState( jf.getExtendedState() | JFrame.MAXIMIZED_BOTH );
        }
        else
        {
            /* Adjust window dimensions not to exceed screen dimensions ...
             */
            Dimension win = new Dimension( 1024, 600 );
            Dimension scsz = Toolkit.getDefaultToolkit().getScreenSize();
            win.width  = Math.min( win.width, scsz.width );
            win.height = Math.min( win.height, scsz.height - 40 );
            jf.setSize( win );
            
            /* ... then center window on the screen.
             */
            jf.setLocation( ( scsz.width - win.width )/2, 
                            ( scsz.height - 40 - win.height )/2 );

            logPane.setVisible( true );
        }

        jf.setVisible( true );
        jf.toFront(); 
    }

    /**
     *  Creates log area for local print-outs.
     */
    private void createLogArea ()
    {
        synchronized( this )
        {
            if ( logf != null ) {
                return;
            }
                
            /* Log area GUI component
             */
            logf = new JTextArea ();
            
            logf.setLineWrap( true );
            logf.setWrapStyleWord( true );
            logf.setEditable( false );
            logf.setPreferredSize( new Dimension( 250, 0 ) );
            logf.setFont( new Font( Font.MONOSPACED, Font.PLAIN, 14 ) );
            logf.setBackground( new Color( 255, 255, 192 ) );
            logf.setForeground( new Color(   0,   0, 192 ) );
            logf.addKeyListener( this );
            
            /* Log area is scrollable...
             */
            logPane = new JScrollPane( logf );
            logPane.setHorizontalScrollBarPolicy(
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
            add( logPane, BorderLayout.EAST );
        }
    }

    /**
     *  Clears log area
     * 
     */
    public void clearLogArea ()
    {
        synchronized( this ) 
        {
            if ( logf != null ) {
                logf.selectAll ();
                logf.replaceSelection( "" );
                logf.setRows( 0 );
            }
        }
    }
    
    /**
     *  Logs a single line of message.
     *  
     *  @param str  contains message to be shown
     */
    public void println( String str )
    {
        synchronized( this )
        {
            System.out.println( str );

            if ( logf == null ) {
                return;
            }

            logf.append( str + "\n" );
            logf.setRows( logf.getRows () + 1 );
            logf.setCaretPosition( logf.getText().length () );
        }
    }
    
    /**
     *  Starts T1 & T2 tests (delayed for spec. time) in separate thread.
     *  
     *  @param delaySec  delay in seconds
     */
    public void startTests( float delaySec )
    {
        synchronized( this )
        {
            if ( testerThread == null )
            {
                testerThread = new T3( this, 5.0f, delaySec );
                testerThread.start ();
            }
        }
    }

    /**
     *  Adjusts view to current number of particles in the underlying world. 
     *  Triggered by <code>WorldOfParticles</code> after some particles are
     *  added or removed from the world.
     *
     *  <strong>Warning:</strong> Method is synchronized (before called) in 
     *  <code>WorldOfParticles</code> context. To avoid dead-locks, do not use 
     *  <code>synchronized</code> inside the method.
     *  
     *  @param newParticleCount  current particle count in the instance of
     *                           the <code>WorldOfParticles</code>
     */
    @Override
    public void onParticleCountChanged( int newParticleCount )
    {
        /* Suppress 'force annotations' when particle count exceeds some limit
         * then enable them back when it drops down bellow some (other) limit
         */
        if ( annotateForces && newParticleCount > 20 ) {
            annotateForces = false;
        } else if ( ! annotateForces && newParticleCount < 15 ) {
            annotateForces = true;
        }

        /* Keep CPU happy reducing the frame-rate if there are too many particles
         * to render.
         */
        if ( animationTimerResolution < 20 && newParticleCount > 40 ) {
            startAnimation( 20 );
        } else if ( animationTimerResolution >= 20 && newParticleCount < 40 ) {
            startAnimation( 5 );
        }
    }

    /**
     *  Gets boundaries of infinite potential barrier keeping particles together.
     */
    @Override
    public WorldOfParticles.Barrier getBarrier ()
    {
        int logWidth = logPane != null && logPane.isVisible () ? logPane.getWidth () : 0;
        
        return world.new Barrier( 
                15, 15, getWidth () - logWidth - 15, getHeight () - 15 
                ); // component width reduced for the log area and 15 pixels around
    }

    /**
     *  Gets the GraphicsConfiguration associated with parent Component.
     * 
     * @return the GraphicsConfiguration used by parent's Component or null
     */
    @Override
    public GraphicsConfiguration getGraphicsConfiguration ()
    {
        return super.getGraphicsConfiguration ();
    }

    /**
     *  Creates pairs of charged particles. Particles are not created
     *  after exceeding some limit.
     *  
     *  @param count     number of particle pairs to create
     *  @param lifeTime  particle life-time in seconds
     */
    public void createPairOfChargedParticles( int count, double lifeTime )
    {
        if ( world.getParticleCount () >= maxParticlesAllowed ) {
            return;
        }

        for ( int i = 0; i < count; ++i )
        {
            double x = ( getWidth  () - 100 ) * Math.random ();
            double y = ( getHeight () - 100 ) * Math.random ();
            
            world.addNewParticle( 1.0, +1.0, // mass and charge
                    x + 100 * Math.random (), // x-pos
                    y + 100 * Math.random (), // y-pos
                    0.0, // radius 0 == random
                    lifeTime + Math.random () * 2 // life time
                    );
            
            world.addNewParticle( 1.0, -1.0, // mass and charge
                    x - 100 * Math.random (),
                    y - 100 * Math.random (),
                    0.0, // radius 0 == random
                    lifeTime + Math.random () * 2 // life time
                    );
        }
    }

    /**
     *  Call-back from T3 after all tests have been completed.
     */
    public void afterTestsCompleted ()
    {
        synchronized( this )
        {
            testerThread = null; // new tests won't start if testerThread != null

            println( "" );
            println( "Press H for usage..." ); // remind user...
            println( "" );
        }
    }

    /** 
     *  Paints the component i.e. renders the world of particles
     */
    @Override
    public void paintComponent( Graphics g )
    {
        /* Erase the background
         */
        g.setColor( new Color( backgroundColor, backgroundColor, backgroundColor ) );
        g.fillRect( 0, 0, getWidth(), getHeight() );

        /* Engrave background with the information such as particle count,
         * frame-rate and time scale
         */
        {
            int x = 10, y = 0, dy = 20; // position and delta

            g.setColor( Color.BLUE );
            
            g.drawString(
                    String.format( "N = %d particles", world.getParticleCount () ),
                    x, ( y += dy )
                    );
            
            g.drawString(
                    String.format( "Frame-rate: %2$.0f Hz (%1$d ms)", 
                        animationTimerResolution, 1000f / (float)animationTimerResolution
                        ),
                    x, ( y += dy )
                    );
            
            g.drawString(
                    String.format( "Time scale: %.2f", 
                        world.getTimeScale ()
                        ),
                    x, ( y += dy )
                    );

            if ( isFullScreen () ) {
                g.setColor( Color.GRAY );
                
                g.drawString( "Press ESC to exit fullscreen mode...",
                        x, ( y += dy )
                        );
            }
        }

        /* Finally, render instance of the world of particles.
         */
        world.paint( g, annotateForces );
    }

    /**
     *  Handles events from the Swing timer. Timer is used to render animation frames
     *  at specific animation frame rate.
     */
    @Override
    public void actionPerformed( ActionEvent ae )
    {
        /* Do not allow an empty worlds
         */
        if ( world.getParticleCount () == 0 )
        {
            /* Create one huge mass
             */
            hugeMass = world.addNewParticle( 
                    1e12, 0.0, // mass and charge
                    getWidth ()/2 - 200, getHeight () / 2, 25.0, // position and radius
                    Double.POSITIVE_INFINITY // life time
                    ); 

            /* Create one slightly large mass as a satellite
             */
            world.addNewParticle( 
                    1e11, 0.0, // mass and charge
                    getWidth ()/2, getHeight () / 2, 20.0, // position and radius
                    Double.POSITIVE_INFINITY // life time
                    ); 

            /* Create an initial pair of charged particles
             */
            createPairOfChargedParticles( 1, Double.POSITIVE_INFINITY );
        }

        /* Fade-in/out background depending on number of particles
         * (keeping backgroundColor *always* in valid range while changing).
         */
        int alpha = backgroundColor;
        
        if ( world.getParticleCount () > 35 && alpha > 102 ) {
            --alpha;
        }
        else if ( world.getParticleCount () > 55 && alpha > 0 ) {
            --alpha;
        }
        else if ( world.getParticleCount () < 55 && alpha < 102 ) {
            ++alpha;
        }
        else if ( world.getParticleCount () < 20 && alpha < 255 ) {
            ++alpha;
        }

        backgroundColor = alpha < 0 ? 0 : alpha > 255 ? 255 : alpha;
        
        repaint ();
    }
    
    /**
     *  Moves the frame-rate up/down by changing the timer resolution
     *  
     *  @param delta  delta timer resolution in millis
     */
    private void changeAnimationResolution( int delta ) {

        int newResolution = animationTimerResolution;
        
        newResolution += delta;

        newResolution = Math.max( newResolution, 0 );
        newResolution = Math.min( newResolution, 500 );
        
        startAnimation( newResolution );
    }
    
    /**
     *  Starts the animation if not already started, and changes frame-rate
     *  
     *  @param resolution   frame rate timer resolution in millis
     */
    private void startAnimation( int resolution ) 
    {
        if ( timer != null ) {
            timer.stop ();
            timer.setDelay( resolution );
        } else {
            timer = new Timer( resolution, this );
        }
        
        animationTimerResolution = resolution;
        
        timer.start ();
    }

    /**
     *  Implements <code>KeyListener</code>'s key pressed event.
     *  Toggles various rendering flags.
     */
    @Override
    public void keyPressed( KeyEvent ke ) {
        
        int keyCode = ke.getKeyCode ();
        
        switch( keyCode )
        {
        case KeyEvent.VK_A:
            /* create a pair of charged particles with life-time 20 ± 10 sec */
            createPairOfChargedParticles( 1, 15.0 + Math.random () * 10.0 );
            break;
            
        case KeyEvent.VK_B:
            /* create five pairs of charged particles with life-time 10 ± 2.5 sec */
            createPairOfChargedParticles( 5, 7.5 + Math.random () * 5.0 );
            break;
            
        case KeyEvent.VK_C:
            createPairOfChargedParticles( 50, 60 + Math.random () * 20.0 );
            break;
            
        case KeyEvent.VK_D:
            world.dump (); // dump to System.out all particle physical props.
            break;

        case KeyEvent.VK_F:
            toggleFullScreen ();
            break;

        case KeyEvent.VK_ESCAPE:
            exitFullScreen ();
            break;
            
        case KeyEvent.VK_H:
            printUsage ();
            break;
            
        case KeyEvent.VK_E:
            world.makeCentripetalVelocities ();
            break;
            
        case KeyEvent.VK_R:
            world.resetVelocities (); // set all velocities to 0
            break;
            
        case KeyEvent.VK_O:
            world.incTimeScale( -0.1 ); // slow-down world's time
            break;
            
        case KeyEvent.VK_P:
            world.incTimeScale( +0.1 ); // speed-up world's time
            break;
            
        case KeyEvent.VK_UP:
            if ( hugeMass != null ) {
                hugeMass.moveParticle( 0, -10 );
            }
            break;
            
        case KeyEvent.VK_DOWN:
            if ( hugeMass != null ) {
                hugeMass.moveParticle( 0, +10 );
            }
            break;
            
        case KeyEvent.VK_LEFT:
            if ( hugeMass != null ) {
                hugeMass.moveParticle( -10, 0 );
            }
            break;
            
        case KeyEvent.VK_RIGHT:
            if ( hugeMass != null ) {
                hugeMass.moveParticle( +10, 0 );
            }
            break;
            
        case KeyEvent.VK_T:
            startTests( 0f );
            break;

        case KeyEvent.VK_N:
            annotateForces = ! annotateForces;
            break;

        case KeyEvent.VK_SPACE:
            world.togglePaused (); // stop/continue physical calculations
            break;

        case KeyEvent.VK_PLUS:
        case KeyEvent.VK_ADD:
            changeAnimationResolution( +5 ); // Slow-down frame rate
            break;

        case KeyEvent.VK_MINUS:
        case KeyEvent.VK_SUBTRACT:
            changeAnimationResolution( -5 ); // Speed-up frame rate
            break;
        }
    }

    /**
     *  Implements <code>KeyListener</code>'s key released event.
     */
    @Override
    public void keyReleased( KeyEvent ke )
    {
        /* unused */
    }
    
    /**
     *  Implements <code>KeyListener</code>'s key typed event.
     */
    @Override
    public void keyTyped( KeyEvent ke )
    {
        /* unused */
    }

}

/*! 
 *  \mainpage Multi-threaded World of Particles (WOP)
 *
 *  \section s_intro Introduction
 *  
 *  The package implements solution to \ref p_task as a part of 
 *  the <a href="http://dsv.su.se/utbildning/distans/ip1" target="_blank"><b>SU/IP1 
 *  course</b></a>.
 *  
 *  \image html wopApp.png
 *  
 *  \section s_desc Description
 *  
 *  The application displays an artificial world of charged and electrically neutral
 *  particles that are governed by physical laws (i.e. electrical and gravitational 
 *  forces as well Newton's law of motions). This world is referred in the rest of 
 *  the documentation as the <b>World of Particles</b> or simply the <b>WOP</b>.
 *  
 *  The motion for each particle in WOP is calculated in its own separate thread 
 *  Particle.run() belonging to the instance of Particle class. 
 *  The interacting forces between particles are calculated in
 *  WorldOfParticles.run() thread common for all particles belonging to
 *  the same WorldOfParticles. 
 *  Instance of the WorldOfParticles is rendered (and animated) 
 *  inside (as a part of) the JPWorld, which extends 
 *  <a href="http://download.oracle.com/javase/6/docs/api/javax/swing/JComponent.html">
 *  <b>JComponent</b></a>. Particles renders themselves with Particle.paint() in
 *  the JPWorld's graphical context.
 *  
 *  Further, there are three separate classes T1, T2 and T3 
 *  that control creation of particles in the WOP (see T1.run, T2.run and T3.run)
 *  and as such they demonstrate a solution to \ref p_task.
 *
 *  \section s_jar Executable
 *  
 *  The jar file of the package can be found <a href="../wop.jar"><b>here</b></a>.
 *  
 *  <div style="color:darkred;font-weight:bold;">
 *  Application can be run both as stand-alone and as an applet.</div> 
 *  
 *  Use RunAsApplet class to start application in applet mode.
 *  
 *  \section s_src Source Files
 *  
 *   - \ref T1.java
 *   - \ref T2.java
 *   - \ref T3.java
 *   - \ref JPWorld.java
 *   - \ref WorldOfParticles.java
 *   - \ref Particle.java
 *   - \ref RunStandalone.java
 *   - \ref RunAsApplet.java
 */
/*! 
 *  \page p_task IP1-1.1 Uppgift
 *  
 *  Gör ett program som består av tre klasser:
 *   - En huvudklass med en main-metod
 *   - En trådad klass T1 som ärver av klassen Thread
 *   - En trådad klass T2 som implmenterar interfacet Runnable
 *
 *  Trådarna ska en gång per sekund skriva ut följande på STDOUT (kommandofönstret):
 *   - Tråd T1: Tråd 1
 *   - Tråd T2: Tråd 2
 *
 *  Frivillig utökning är att även titta på hur man pausar trådar och då utöka programmet
 *  ovan så huvudklassen gör följande:
 *  
 *   -#  Skapa och starta en tråd från klass T1 
 *   -#  Vänta i 5 sekunder
 *   -#  Skapa och starta en tråd från klass T2
 *   -#  Vänta i 5 sekunder
 *   -#  Pausa tråden från klass T2
 *   -#  Vänta i 5 sekunder
 *   -#  Aktivera tråden från klass T2
 *   -#  Vänta i 5 sekunder
 *   -#  Stoppa tråden från klass T1
 *   -#  Vänta i 5 sekunder
 *   -#  Stoppa tråden från klass T2
 *   
 *  En annan frivillig utökning är att göra ett program som använder trådar och grafik. 
 */

