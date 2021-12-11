package PSO;

import genius.core.Domain;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.Bid;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

import java.util.*;

public class MyPos {

    int n; // number of particles
    Particle[] particles;
    Particle[] bestParticles;
    double w;//惯性系数

    Particle gBest; // global maximization
    double vMax; // maximised value
    int c1, c2; // 个体最优的学习参数和全局最优的学习参数
    List<Bid> BestBidList; //原始的bid list Ranking
    Random random;

    public MyPos(int n, UserModel userModel) {
        this.n = n;
        c1 = c2 = 2;
        initParticles(userModel);
        updateBest(particles[0]);
        BestBidList = userModel.getBidRanking().getBidOrder();
        setFitness();
        w = 0.5;
        random = new Random();
    }

    //计算两个utitlityspace的差
    private void updateV(int index) {
        double[] v = particles[index].v;
        AdditiveUtilitySpace present = (AdditiveUtilitySpace) particles[index].abstractUtilitySpaces;
        AdditiveUtilitySpace presentBest = (AdditiveUtilitySpace) bestParticles[index].abstractUtilitySpaces;
        AdditiveUtilitySpace best = (AdditiveUtilitySpace) gBest.abstractUtilitySpaces;
        int issueSIze = particles[index].abstractUtilitySpaces.getDomain().getIssues().size();//有多少个issue

        AdditiveUtilitySpaceFactory newAddictiveUtilitySpace = new AdditiveUtilitySpaceFactory(present.getDomain());
        int iterNumV = 0; //吐槽一下这里，v是一个数组所以算位置
        for (Issue issue_ : present.getDomain().getIssues()) {
            IssueDiscrete issue = (IssueDiscrete) issue_;
            double presentBestWeight = presentBest.getWeight(issue);
            double presentWeight = present.getWeight(issue);
            double bestWeight = best.getWeight(issue);
            //更新一个weight
            double vNext = w * v[iterNumV] +
                    c1 * random.nextDouble() * (presentBestWeight - presentWeight)
                    + c2 * random.nextDouble() * (bestWeight - presentWeight);
            particles[index].v[iterNumV] = vNext;
            double valueNumber = Double.max(presentWeight + vNext, Double.MIN_VALUE);
            newAddictiveUtilitySpace.setWeight(issue, valueNumber);
            iterNumV++;

            //更新一下这个issue下面的value
            for (ValueDiscrete value : issue.getValues()) {
                //这里太抽象了，value值在evaluator下面，要通过evaluator的getValue方法获得值，这个值还是个int
                // 调试看一下是不是int
                // 貌似解决了 先试用正则化正则 0~1 然后再更新
                EvaluatorDiscrete presentEvaluatorDiscrete = (EvaluatorDiscrete) present.getEvaluator(issue);
                EvaluatorDiscrete presentBestEvaluatorDiscrete = (EvaluatorDiscrete) presentBest.getEvaluator(issue);
                EvaluatorDiscrete BestEvaluatorDiscrete = (EvaluatorDiscrete) best.getEvaluator(issue);

                Double presentBestValue = presentBestEvaluatorDiscrete.getDoubleValue(value);
                Double presentValue = presentEvaluatorDiscrete.getDoubleValue(value);
                Double bestValue = BestEvaluatorDiscrete.getDoubleValue(value);

                double vNextValue = w * v[iterNumV] +
                        c1 * random.nextDouble() * (presentBestValue - presentValue) +
                        c2 * random.nextDouble() * (bestValue - presentValue);
                particles[index].v[iterNumV] = vNextValue;
                double valueNumber2 = Double.max(presentValue - vNextValue, Double.MIN_VALUE);

                newAddictiveUtilitySpace.setUtility(issue, value, valueNumber2);
                iterNumV++;
            }
        }
        particles[index].abstractUtilitySpaces = newAddictiveUtilitySpace.getUtilitySpace();
    }

    public void iterParticles() {
        for (int i = 0; i < particles.length; i++) {
            updateV(i);
        }
        setFitness();
        //全部的更新之后再求一次全局最优
        System.out.println("这轮建模完事了：" + "最好的结果是:" + gBest.f + "\n");
    }

    public void iterMultipleTimes(int maximum) {
        for (int i = 0; i < maximum; i++) {
            double a = particles[0].abstractUtilitySpaces.getUtility(BestBidList.get(55));
            for (Bid b : BestBidList) {
                System.out.printf(String.valueOf(particles[0].abstractUtilitySpaces.getUtility(b)) + " ");
            }
            System.out.println();
            iterParticles();
            for (Bid b : BestBidList) {
                System.out.printf(String.valueOf(particles[0].abstractUtilitySpaces.getUtility(b)) + " ");
            }
            System.out.println();
        }
    }

    //获得全局最优
    private void getBestParticles() {
        double maxf = gBest.f;
        for (Particle p : particles) {
            if (p.f < maxf) {
                updateBest(p);
                maxf = p.f;
            }
        }
    }

    private void updateBest(Particle p) {
        gBest = p;
    }

    void initParticles(UserModel userModel) {
        particles = new Particle[n];
        bestParticles = new Particle[n];

        for (int i = 0; i < n; i++) {
            particles[i] = new Particle(userModel.getDomain());
            bestParticles[i] = particles[i];
        }
    }

    void setFitness() {
        List<Integer> BIdRankingLableOfParticle;
        for (Particle p : particles) {
            BIdRankingLableOfParticle = getParticleBidList(p);
            int errorPow = 0;
            for (int i = 0, size = BIdRankingLableOfParticle.size(); i < size; i++) {
                int error = (BIdRankingLableOfParticle.get(i) - i);
                errorPow += error * error;
            }

            p.f = changeErrorToFitness(errorPow);
        }

        for (int i = 0; i < particles.length; i++) {
            if (particles[i].f > bestParticles[i].f) {
                bestParticles[i] = particles[i];
            }
        }

        getBestParticles();
    }

    private double changeErrorToFitness(double error) {
        double score = 0;
        double x = error / (Math.pow(BestBidList.size(), 3));
        double theta = -15 * Math.log(x + 0.00001f);  //利用对数思想   -15
        score = theta;
        return score;
    }

    private List<Integer> getParticleBidList(Particle p) {
        List<Double> utilityList = new ArrayList<>();
        List<Bid> newbidRanking = new ArrayList<>();
        for (Bid b : BestBidList) {
            utilityList.add(p.abstractUtilitySpaces.getUtility(b));
        }
        TreeMap<Integer, Double> utilityRank = new TreeMap<>();
        for (int i = 0; i < utilityList.size(); i++) {
            utilityRank.put(i, utilityList.get(i));
        }
        //4. 此时我们需要根据TreeMap的值进行排序（值中存放的是效用值）
        Comparator<Map.Entry<Integer, Double>> valueComparator = Comparator.comparingDouble(Map.Entry::getValue);
        // map转换成list进行排序
        List<Map.Entry<Integer, Double>> listRank = new ArrayList<>(utilityRank.entrySet());
        // 排序
        Collections.sort(listRank, valueComparator);
        //用以上的方法，TreeMap此时就被转换成了List。这tm什么方法我也很烦躁。。
        //list现在长这个样子。[100=0.3328030236029489, 144=0.33843867914476017, 82=0.35366230775310603, 68=0.39994535024458255, 25=0.4407324473062739, 119=0.45895568095691974,
        //不过这也有个好处。就是列表的索引值，可以表示为utilityList的索引值。
        List<Integer> bidRankingLabel = new ArrayList<Integer>();
        for (int i = 0, size = listRank.size(); i < size; i++) {
            bidRankingLabel.add(listRank.get(i).getKey());
        }
        return bidRankingLabel;
        //返回一个list, list里面的item是一个Map,表示这个位置的bid在我们预测的序列中第几排名
    }
//    public void fitnessFunction(){
//        for (int i=0;i<n;++i){
//            particles[i].f=0;
//        }
//    }
}


class Particle {
    public AdditiveUtilitySpace abstractUtilitySpaces;
    public Domain domain;
    public double f; //fitness value
    public double[] v; //速度
    public int size;
    public double vmax = 1.0;

    public Particle(Domain domain) {
        size = 0;
        f = 0;
        this.domain = domain;
        abstractUtilitySpaces = getRandomChromosome();
        v = new double[size];
        this.vmax = 1.0f;
        initializeArrayV();
    }

    //产生一个随机的效用空间
    private AdditiveUtilitySpace getRandomChromosome() {
        Random random = new Random();
        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(domain);  //直接获得当前utilitySpace下的domain.
        List<Issue> issues = additiveUtilitySpaceFactory.getDomain().getIssues();
        size += issues.size();
        for (Issue issue : issues) {
            additiveUtilitySpaceFactory.setWeight(issue, random.nextDouble());    //设置每个issue的权重
            IssueDiscrete values = (IssueDiscrete) issue;       //将issue强制转换为values集合
            size += ((IssueDiscrete) issue).getNumberOfValues();
            for (ValueDiscrete value : values.getValues()) {
                //通过values集合，获取每个value。
                additiveUtilitySpaceFactory.setUtility(issue, value, random.nextDouble());
                //因为现在是累加效用空间，随便设置一个权重之后，可以对当前这个value设置一个效用，效用随机。

            }
            //当效用确定了之后，当前的value自己本身的值也就确定了。
            //这里设置的效用是设置value的evaluation
        }
        additiveUtilitySpaceFactory.normalizeWeights(); //因为之前对每个value的效用值计算都是随机的，这个时候，需要归一化。
        return additiveUtilitySpaceFactory.getUtilitySpace();  //生成一个效用空间之后，返回这个效用空间。
    }

    private void initializeArrayV() {
        for (int i = 0; i < v.length; i++) {
            v[i] = 0f + (vmax - 0f) * Math.random();
        }
    }
}
