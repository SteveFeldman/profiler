package app2test;

import java.util.ArrayList;

/**
 * Very simple app for profiler testing
 * No comments, just check the sources
 */
public class TestApp {

    private static class TestThread extends Thread {

        final int id;

        TestThread( int id ) {
            this.id = id;
        }

        @Override public void run() {
            try {
                TestHandlers handler = new TestHandlers();

                switch (id) {
                    case 0:
                        handler.handler_0_ok();
                        return;
                    case 1:
                        handler.handler_3_ok(1, 1.0, "str");
                        return;
                    case 2:
                        handler.handler_0_ret();
                        return;
                    case 3:
                        handler.handler_0_ex();
                        return;
                }

            }
            catch(RuntimeException ex) {
                if (id != 3) // For id == 3 exception is expected
                    ex.printStackTrace();
            }
        }
    }

    public static void main(String [] args) {

        try {
            ArrayList<Thread> thrs = new ArrayList<>();

            for (int id = 0; id < 4; id++) {
                thrs.add(new TestThread(id));
            }

            for (Thread th : thrs)
                th.start();

            for (Thread th : thrs)
                th.join(5000);

            System.err.println("Done");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
