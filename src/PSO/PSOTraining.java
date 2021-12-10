package PSO; /**
 *
 Copyright 2021 JunjieZhou

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/**
 * 使用粒子群算法计算某个函数的最优解 粒子群模型 ： f‘ = f + omega * v + a*rand()*(fbest - f) +
 * b*rand()*(gbest - f)
 *
 * 寻找 y = -x*x+1 的最大解，
 *
 * 1. 粒子的数量、omega、a、b是自己规定的。
 * 其中omega为粒子有多大程度保持自己的速度：此处可以理解为x的变化程度，即每次x相对于上次的速度变化多少
 * a可以理解为粒子有多大程度保持自己取最大值时的x b可以理解为粒子有多大程度向目前取最大值的x靠拢
 *
 * 2. 粒子最初始的速度△x、粒子最初始的位置x。
 *
 * 3. 粒子fbest的初始值是粒子的x。
 *
 * 4. gbest的初始值为粒子中y最大的x
 *
 */
public class PSOTraining {

    static double questionExpression(double x) {
        return -x * x + 1;
    }

    public static void main(String[] args) {
        // 粒子搜找的次数
        int times = 1000;
        // 粒子的数量
        int n = 1000;
        // 粒子的速度x的变化程度
        double v[] = new double[n];
        // 粒子的位置x
        double p[] = new double[n];
        // 每个粒子走过的路劲中取最优解时的x
        double pBest[] = new double[n];
        // 所有粒子中的最优解即y取最大值时的x
        double gBest = -10;
        // 粒子的x变化程度、保持自己的最大值的程度、向群体最大值倾斜的程度，此处都用随机数代替
        double omega = Math.random();
        double a = Math.random();
        double b = Math.random();

        // 初始化
        for (int i = 0; i < n; i++) {
            // 每个粒子的初始速度都是随机的
            v[i] = Math.random();
            // 每个粒子的初始位置是随机的
            p[i] = Math.random();
            // 每个粒子的初始最大值的位置即为粒子的初始位置
            pBest[i] = v[i];
            // 粒子群初始最大值时的x值
            if (questionExpression(gBest) <= questionExpression(pBest[i])) {
                gBest = pBest[i];
            }
        }

        // 开始进行times次数的迭代
        for (int i = 0; i < times; i++) {
            // 对每个粒子计算此时的速度x值，以及每个粒子取最大值时候的x值，以及粒子群取最大值时的x值
            for (int j = 0; j < n; j++) {
                v[j] = omega * v[j] + a * (pBest[j] - p[j]) + b * (gBest - p[j]);
                p[j] += v[j];
                if (questionExpression(p[j]) >= questionExpression(pBest[j])) {
                    pBest[j] = p[j];
                }
            }

            // 对粒子群计算gBest即目前取y最大值时候的x值
            for (int j = 0; j < n; j++) {
                if (questionExpression(p[j]) >= questionExpression(gBest)) {
                    gBest = p[j];
                }
            }

            System.out.println("current best solution is " + questionExpression(gBest) + " composed by " + gBest);
        }
    }
}