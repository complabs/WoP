
import java.awt.BorderLayout;
import javax.swing.JApplet;


/**
 *  Runs instance of the <code>JPWorld</code> component inside the applet.
 *  
 *  @author Mikica B Kocic
 */
public class RunAsApplet extends JApplet
{
    /**
     *  Implements java.io.Serializable interface
     */
    private static final long serialVersionUID = 6183165032705642517L;

    /**
     *  Main component is instance of the <code>JPWorld</code>.
     */
    private JPWorld wopComponent = new JPWorld ();

    /**
     *  Adds main component <code>JPWorld</code> to the content pane.
     */
    @Override
    public void init () 
    {
        super.init ();
        
        setSize( 1024, 600 );
        getContentPane ().add( wopComponent, BorderLayout.CENTER );
        validate ();
        
        /* Start T1 & T2 tests delayed for 1 sec
         */
        wopComponent.startTests( 1f /*sec delay*/ );
    }
}
