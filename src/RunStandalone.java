
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;


/**
 *  Runs instance of the <code>JPWorld</code> component inside
 *  the stand-alone application with own <code>JFrame</code>.
 *  
 *  @author Mikica B Kocic
 */
public class RunStandalone extends JFrame
{
    /**
     *  Implements java.io.Serializable interface
     */
    private static final long serialVersionUID = -6852878245259708233L;

    /**
     *  Main component is instance of the <code>JPWorld</code>.
     */
    private JPWorld wopComponent = new JPWorld ();

    /**
     *  Adds main component <code>JPWorld</code> to the content pane.
     */
    public RunStandalone ()
    {
        super(
            "IP1-1.1: Multithreaded World of Particles (inefficient but educational)"
            );
        
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

        /* Adjust window dimensions not to exceed screen dimensions ...
         */
        Dimension win = new Dimension( 1024, 600 );
        Dimension scsz = Toolkit.getDefaultToolkit().getScreenSize();
        win.width  = Math.min( win.width, scsz.width );
        win.height = Math.min( win.height, scsz.height - 40 );
        setSize( win );
        setMinimumSize( new Dimension( 330, 100 ) );
        
        /* ... then center window on the screen.
         */
        setLocation( ( scsz.width - win.width )/2, ( scsz.height - 40 - win.height )/2 );
        
        /*  Register the thread tester component, then make frame visible
         */
        add( wopComponent );
        addKeyListener( wopComponent );
        setVisible( true );
        
        /* Start T1 & T2 tests delayed for 1 sec
         */
        wopComponent.startTests( 1f /*sec delay*/ );
    }
    
    /**
     *  Main program entry that creates and shows GUI.
     *  
     *  @param args   main program arguments
     */
    public static void main( String[] args ) 
    {
        /*  Create and show GUI
         */
        Runnable doCreateAndShowGUI = new Runnable () {
            public void run () {
                new RunStandalone ();
            }
        };

        SwingUtilities.invokeLater( doCreateAndShowGUI );
    }
}
