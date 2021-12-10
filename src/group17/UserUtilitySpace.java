package group17;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.misc.Pair;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AdditiveUtilitySpace;

import java.util.*;

public class UserUtilitySpace extends AdditiveUtilitySpace {

    private UserModel userModel;
    private List<Bid> bidList;
    private double[][] frequency;
    private double[][] option_value;
    private double[] weights;

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


}
