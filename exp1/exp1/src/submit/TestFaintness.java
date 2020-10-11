package submit;

import java.io.IOException;

class TestFaintness {
    /**
     * In this method all variables are faint because the final value is never used.
     * Sample out is at src/test/Faintness.out
     */
    void test1() {
        int x = 2;
        int y = x + 2;
        int z = x + y;
        return;
    }

    /**
     * 
     * 
     */
    int test2() {
        int x = 2;
        int y = x + 2;
        int z = x + y;
        return z;
    }

    /**
     * x,m faint; z,y not faint
     */
    void test3() throws IOException {
        int x = 2;
        int y = 4;
        int z = 2 + y;
        int m = x + z;
        if (System.in.read() > 2) {
            System.out.println(z);
        } else {
            System.out.println("hahah");
        }
    }

    /**
     * x and z is not faint variant
     * in z += x,x was ignored thus x is faint variant
     */
    void test4() {
        int x = 2;
        int z = 0;
        for (int i = 0; i < 8; i++) {
            z += x;
        }
        System.out.println(z);
    }
}
