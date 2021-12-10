package group17;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.misc.Pair;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;

import java.util.*;

public class UserUtilitySpace extends AdditiveUtilitySpace {

    private UserModel userModel;
    private List<Bid> bidList;
    private double[][] frequency;
    private double[][] option_value;
    private double[] weights;
    private Random random;

    public UserUtilitySpace(AdditiveUtilitySpace us, UserModel userModel) {
        super(us);
        this.userModel = userModel;
        bidList = userModel.getBidRanking().getBidOrder();
        weights = new double[userModel.getDomain().getIssues().size()];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = new Random().nextDouble();
        }
        normalize();
        update_matrix();
        random = new Random();
    }

    private void Update() {
        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(userModel.getDomain());
        for (Bid i : bidList) {
            System.out.println(i);
        }
    }

    public void UpdateUserModel(UserModel userModel) {
        this.userModel = userModel;
        this.bidList = userModel.getBidRanking().getBidOrder();
    }

    /*
    Option value 使用jhonny black 建模
     */
    private void update_matrix() {
        List<Issue> issues = userModel.getDomain().getIssues();
        int value_number = ((IssueDiscrete) userModel.getDomain().getIssues().get(0)).getValues().size();
        frequency = new double[issues.size()][value_number];
        option_value = new double[issues.size()][value_number];
        //how many line segments
        double nLineSegments = 100.0;
        for (int i = 0; i < bidList.size(); i++) {
            Bid b = bidList.get(i);
            double factor = i % Math.round((double) bidList.size() / nLineSegments) + 1;
            for (Map.Entry<Integer, Value> entry : b.getValues().entrySet()) {
                IssueDiscrete issue = (IssueDiscrete) b.getIssues().get(entry.getKey() - 1);
                int value_index = issue.getValueIndex((ValueDiscrete) entry.getValue());
                frequency[entry.getKey() - 1][value_index] += 1 * factor;
            }
        }

        for (int i = 0; i < frequency.length; i++) {
            //获得top n
            Integer[] index = IndexArray(frequency[i]);
            //获得这一行的数据
            for (int k = 0; k < index.length; k++) {
                // option value是: (n-top_n +1) / m
                option_value[i][k] = (index.length - (double) index[k]) / index.length;
            }
        }

    }

    public double[][] getFrequency() {
        return frequency;
    }

    public double[][] getOption_value() {
        return option_value;
    }

    private Integer[] IndexArray(double[] arr) {
        Double[] arr_double = Arrays.stream(arr).boxed().toArray(Double[]::new);
        List<Double> arrayList = Arrays.asList(arr_double);

        Set<Double> set = new HashSet<>(arrayList);
        arrayList = new ArrayList<>(set);
        arrayList.sort(Comparator.reverseOrder());

        int[] index_array = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            index_array[i] = arrayList.indexOf(arr[i]);
        }
        //把integer array 转化为 Integer array
        Integer[] integers = Arrays.stream(index_array).boxed().toArray(Integer[]::new);
        return integers;
    }

    private void normalize() {
        double sum = Arrays.stream(weights).sum();
        for (int i = 0; i < weights.length; i++) {
            weights[i] /= sum;
        }
    }

    //产生一个随机的效用空间
    private AbstractUtilitySpace getRandomChromosome() {
        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(userModel.getDomain());  //直接获得当前utilitySpace下的domain.
        List<Issue> issues = additiveUtilitySpaceFactory.getDomain().getIssues();
        for (Issue issue : issues) {
            additiveUtilitySpaceFactory.setWeight(issue, random.nextDouble());    //设置每个issue的权重
            IssueDiscrete values = (IssueDiscrete) issue;       //将issue强制转换为values集合
            for (ValueDiscrete value : values.getValues()) {            //通过values集合，获取每个value。
                additiveUtilitySpaceFactory.setUtility(issue, value, random.nextDouble());   //因为现在是累加效用空间，随便设置一个权重之后，可以对当前这个value设置一个效用，效用随机。
            }                                                                                            //当效用确定了之后，当前的value自己本身的值也就确定了。
            //这里设置的效用是设置value的evaluation
        }
        additiveUtilitySpaceFactory.normalizeWeights(); //因为之前对每个value的效用值计算都是随机的，这个时候，需要归一化。
        return additiveUtilitySpaceFactory.getUtilitySpace();  //生成一个效用空间之后，返回这个效用空间。

    }

    private double getFitness(AbstractUtilitySpace abstractUtilitySpace) {
        BidRanking bidRanking = userModel.getBidRanking();   //1.先从userModel中取出bidRanking列表

        //2.我们要单独写一个bidList去存放bidRanking去防止计算量过大。
        List<Bid> bidList = new ArrayList<>();

        //如果bid量小于400
        for (Bid bid : bidRanking) {
            bidList.add(bid);
        }

        List<Double> utilityList = new ArrayList<>();
        for (Bid bid : bidList) {
            utilityList.add(abstractUtilitySpace.getUtility(bid));   //计算在当前空间下，每个bidRanking的实际效用是多少。并且放入utilityList中。
        }                                                             //注意，此时的utilityList的索引和bidRanking的索引是相同的。我们需要利用这个存放在TreeMap中


        TreeMap<Integer, Double> utilityRank = new TreeMap<>();   //构建treeMap，一个存放一下当前的索引，一个存放对应索引的utility。

        for (int i = 0; i < utilityList.size(); i++) {   //这里对utility进行遍历，将索引和效用存放在TreeMap中。
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

        int error = 0;
        for (int i = 0; i < listRank.size(); i++) {
            int gap = Math.abs(listRank.get(i).getKey() - i);  //5. 这里的i其实可以对应utilityList的索引i。假设i=1.此时在utilityList中的效用应该是最低值。
            error += gap * gap;
        }                                             //但是，在listRank中，效用最低的值对应的index竟然是100。那说明，这个效用空间在第一个位置差了很大。
        // 同理，如果listRank中的每一个键能正好接近或者等于它所在的索引数，那么说明这个效用空间分的就很对。

        //6. 对数思想，需要的迭代次数最少
        double score = 0.0f;
        double x = error / (Math.pow(listRank.size(), 3));
        double theta = -15 * Math.log(x + 0.00001f);  //利用对数思想   -15
        score = theta;
        System.out.println("Error:" + error);  //7. 监控每次迭代的error的大小

        return score;  //8. 返回fitness score

    }


}
