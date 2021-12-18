package group26;

import genius.core.Bid;
import genius.core.Domain;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Objective;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

import java.util.*;

public class PreferenceElicitation extends AdditiveUtilitySpaceFactory {

    HashMap<Bid, Double> bidsWithUtility = new HashMap<>();
    BidRanking r;
    List<Bid> bids;
    List<Bid> reverseBids;
    public AdditiveUtilitySpace u;
    double weightChangeStep = 0.0D;
    int size = 0;

    /**
     * Generates an simple Utility Space on the domain, with equal weights and zero values.
     * Everything is zero-filled to already have all keys contained in the utility maps.
     *
     * @param d
     */
    public PreferenceElicitation(Domain d, BidRanking r) {
        super(d);
        List<Issue> issues = d.getIssues();
        int noIssues = issues.size();
        Map<Objective, Evaluator> evaluatorMap = new HashMap<Objective, Evaluator>();
        this.size = r.getSize();
        for (Issue i : issues) {
            IssueDiscrete issue = (IssueDiscrete) i;
            EvaluatorDiscrete evaluator = new EvaluatorDiscrete();
            evaluator.setWeight(1.0 / noIssues);
            for (ValueDiscrete value : issue.getValues()) {
                evaluator.setEvaluationDouble(value, 2);
            }
            evaluatorMap.put(issue, evaluator);
        }
        u = new AdditiveUtilitySpace(d, evaluatorMap);
        this.r = r;
        this.bids = r.getBidOrder();
        reverseBids = new ArrayList<>(bids);
        Collections.reverse(reverseBids);
        weightChangeStep = 1.0D / (200 + size);
        predictBidsWithUtility();
//        for (Issue is : getIssues()) {
//            for (ValueDiscrete v : ((IssueDiscrete) is).getValues()) {
//                System.out.println(v.getValue() + " ===================的值是： " + getUtility(is, v));
//            }
//        }
    }

    @Override
    public void setUtility(Issue i, ValueDiscrete v, double value) {
        EvaluatorDiscrete evaluator = (EvaluatorDiscrete) u.getEvaluator(i);
        if (evaluator == null)
        {
            evaluator = new EvaluatorDiscrete();
            u.addEvaluator(i, evaluator);
        }
        evaluator.setEvaluationDouble(v, value);
    }

    @Override
    public void setWeight(Issue i, double weight) {
        EvaluatorDiscrete evaluator = (EvaluatorDiscrete) u.getEvaluator(i);
        evaluator.setWeight(weight);
    }

    public double getWeight(Issue i) {
        EvaluatorDiscrete evaluator = (EvaluatorDiscrete) u.getEvaluator(i);
        return evaluator.getWeight();
    }

    @Override
    public double getUtility(Issue i, ValueDiscrete v) {
        EvaluatorDiscrete evaluator = (EvaluatorDiscrete) u.getEvaluator(i);
        return evaluator.getDoubleValue(v);
    }

    private void predictBidsWithUtility() {
        double highest = r.getHighUtility();
        double lowest = r.getLowUtility();
        for (int i = 0; i < size; i++) {
            bidsWithUtility.put(bids.get(i), ((highest - lowest) / (size - 1)) * i + lowest);
            System.out.println("==========" + bidsWithUtility.get(bids.get(i)));
        }
    }

    @Override
    public void estimateUsingBidRanks(BidRanking r) {
        for(int i = 0; i < 25; i++) {
            trainingProcess(this.bids);
        }
    }

    public <T> void shuffle(List<T> list) {
        int size = list.size();
        Random random = new Random();

        for(int i = 0; i < size; i++) {
            // 获取随机位置
            int randomPos = random.nextInt(size);

            // 当前元素与随机元素交换
            Collections.swap(list, i, randomPos);
        }
    }


    private void trainingProcess(List<Bid> bids) {
        System.out.println("weightstep ======================= " + this.weightChangeStep);
        Bid previousBid = null;
        for (Bid bid : bids) {
            System.out.println("预测为：" + u.getUtility(bid) + "。 实际为：" + bidsWithUtility.get(bid));
            double bias = u.getUtility(bid) - bidsWithUtility.get(bid);
            System.out.println("误差是: " + bias);
            if (bias == 0) {
                continue;
            }
            if (previousBid != null) {
                List<Issue> diff = new ArrayList<>();
                for (Issue i : getIssues()) {
                    if (bid.getValue(i).equals(previousBid.getValue(i))) {
                        diff.add(i);
                    }
                }
                double change = weightChangeStep / diff.size();
                for (Issue i : getIssues()) {
                    if (diff.contains(i)) {
                        setWeight(i, (weightChangeStep / diff.size()) * (-1 * bias / Math.abs(bias)) + getWeight(i));
                    } else {
                        setWeight(i, (weightChangeStep / (getIssues().size() - diff.size())) * (-1 * bias / Math.abs(bias)) + getWeight(i));
                    }
                }
            }
            for (Issue i : getIssues()) {
                double v = getUtility(i, (ValueDiscrete) bid.getValue(i)) - (bias / getIssues().size() / getWeight(i) / size * 3);
                if (v < 0){
                    v = 0;
                }
                setUtility(i, (ValueDiscrete) bid.getValue(i), v);
            }
            previousBid = bid;
        }
        normalizeWeights();
    }


    private void normalizeWeightsByMaxValues()
    {
        for (Issue i : getIssues())
        {
            EvaluatorDiscrete evaluator = (EvaluatorDiscrete) u.getEvaluator(i);
            evaluator.normalizeAll();
        }
        scaleAllValuesFrom0To1();
        u.normalizeWeights();
    }
    @Override
    public void scaleAllValuesFrom0To1()
    {
        for (Issue i : getIssues())
        {
            EvaluatorDiscrete evaluator = (EvaluatorDiscrete) u.getEvaluator(i);
            evaluator.scaleAllValuesFrom0To1();
        }
    }
    @Override
    public void normalizeWeights()
    {
        u.normalizeWeights();
    }

    public AdditiveUtilitySpace getU() {
        return u;
    }
}
