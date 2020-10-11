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
     * 这个例子中由于z作为返回值，x和y在被使用之前都不是faint的
     * 但在三地址码中，y的计算式中的x被作为常量优化掉了，所以x始终都是faint的
     */
    int test2() {
        int x = 2;
        int y = x + 2;
        int z = x + y;
        return z;
    }

    /**
     * 这个例子中x和m都是faint的，z和y不是faint的
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
     * 这个例子中x和z都不是faint的
     * 但是z += x语句中的x在编译时被优化了，所以输出的分析结果中x还是faint的
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
