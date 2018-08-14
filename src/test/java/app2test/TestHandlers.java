package app2test;

import java.util.ArrayList;

/**
 * Different handlers with different argumnet list and error handlings
 */
public class TestHandlers {
    private static ArrayList<Object> strs = new ArrayList<>();
    public static boolean testF = true;

    public void handler_0_ok() {
        doAction();
    }

    public void handler_3_ok(int p1, Double d, String s) {
        System.out.println("calling handler_3_ok");
        doAction();
    }

    public void handler_0_ret() {
        System.out.println("calling handler_0_ret");
        doAction();

        if (testF)
            return;

        doAction();
    }

    public void handler_0_ex() {
        System.out.println("calling handler_0_ex");
        doAction();

        if (testF)
            throw new RuntimeException("just a test");

        doAction();
    }


    // some dummy action. Let's create some
    private void doAction() {
        try {
            Thread.sleep(500);
            synchronized(strs) {
                strs.add( new String("1") );
            }
            synchronized(strs) {
                strs.add( new StringBuilder("sb").toString() );
            }

            strs.add(new int[100]);

            synchronized(strs) {
                strs.add( new Long(1) );
            }
            synchronized(strs) {
                strs.add( new Double(3.5) );
            }
            synchronized(strs) {
                strs.add( new Integer(3) );
            }
            Thread.sleep(500);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
