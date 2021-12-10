package PSO;

import genius.core.utility.AbstractUtilitySpace;

public class MyPos {

    int n; // number of particles
    AbstractUtilitySpace[] particles;
    Particle[] v;
    Particle[] pBest; //local maximization
    Particle gBest; // global maximization
    double vMax; // maximised value
    int c12; // learning parameters


//    public void fitnessFunction(){
//        for (int i=0;i<n;++i){
//            particles[i].f=0;
//        }
//    }
}


class Particle{
    public double[] w;
    public double[][] v;

    public double f; //fitness value

    public Particle(int NumWeights,int NumValues){
        w = new double[NumWeights];
        v = new double[NumWeights][NumValues];
        f=0;
    }
}
