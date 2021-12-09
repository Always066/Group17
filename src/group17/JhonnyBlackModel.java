package group17;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.misc.Pair;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

import java.util.*;

public class JhonnyBlackModel {
    //initialize the frequency matrix
    private double[][] opponent_frequency;
    private double[][] opponent_option_value;
    private double[] opponent_weight;
    private AdditiveUtilitySpace utilitySpace;
    private Map<Pair<Integer, Value>, Integer> optionCountMap;
    List<Issue> issues;


    public JhonnyBlackModel(AdditiveUtilitySpace additiveUtilitySpace) {
        this.utilitySpace = additiveUtilitySpace;
        init();
    }

    private void init() {
        issues = utilitySpace.getDomain().getIssues();
        int value_number = ((IssueDiscrete) issues.get(0)).getValues().size();
        opponent_frequency = new double[issues.size()][value_number];
        opponent_option_value = new double[issues.size()][value_number];
        opponent_weight = new double[issues.size()];
        optionCountMap = new HashMap<>();
        for (Issue issue : issues) {
            int issueNumber = issue.getNumber();
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
                optionCountMap.put(new Pair<Integer, Value>(issueNumber, valueDiscrete), issueDiscrete.getValueIndex(valueDiscrete));
            }
        }
    }

    public void update_model(Bid lastOffer) {
        //track the frequency
        for (Map.Entry<Integer, Value> entry : lastOffer.getValues().entrySet()) {
            IssueDiscrete issueDiscrete = (IssueDiscrete) issues.get(entry.getKey() - 1);
            opponent_frequency[entry.getKey() - 1][optionCountMap.get(new Pair<>(entry.getKey(), entry.getValue()))] += 1;
        }
        //update option value every interval times
        //更新各种矩阵
        for (int i = 0; i < opponent_frequency.length; i++) {
            //获得top n
            Integer[] index = IndexArray(opponent_frequency[i]);
            //获得这一行的数据
            double[] sub_row = opponent_frequency[i];
            double weights = 0;
            for (int k = 0; k < index.length; k++) {
                // option value是: (n-top_n +1) / m
                opponent_option_value[i][k] = (index.length - (double) index[k]) / index.length;
                //求这一行的和
                double sum = Arrays.stream(sub_row).sum();
                // opponent weight 是 option frequency**2/sum
                weights += (opponent_frequency[i][k] / sum) * (opponent_frequency[i][k] / sum);
            }
            opponent_weight[i] = weights;
        }
        //进行一个正则化(normalize)
        double weight_sum = Arrays.stream(opponent_weight).sum();
        for (int k = 0; k < issues.size(); k++) {
            opponent_weight[k] = opponent_weight[k] / weight_sum;
        }
    }


    //返回这一行的 topn index数组
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

    //根据我们的建模给出对手的预估效用值
    public double valuation_opponent(Bid offer) {
        HashMap<Integer, Value> hashMap = offer.getValues();
        double utility = 0;
        for (int i = 1; i <= hashMap.size(); i++) {
            int option_number = optionCountMap.get(new Pair<>(i, hashMap.get(i)));
            utility += opponent_weight[i - 1] * opponent_option_value[i - 1][option_number];
        }
        return utility;
    }
}