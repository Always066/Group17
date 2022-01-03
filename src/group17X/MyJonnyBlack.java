package group17X;

import com.sun.xml.internal.xsom.impl.scd.Iterators;
import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.NegotiationInfo;
import genius.core.utility.AdditiveUtilitySpace;

import java.io.Serializable;
import java.net.CookieHandler;
import java.util.*;
import java.util.stream.Collectors;

//public class MyJonnyBlack {
//    private List<Issue> issues;
//    private HashMap<Integer, HashMap<String, Integer>> frequencyTable = new HashMap<>();
//    private HashMap<Integer, HashMap<String, Double>> frequencyRateTable = new HashMap<>();
//
//    public MyJonnyBlack(NegotiationInfo info) {
//        AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) info.getUtilitySpace();
//        issues = additiveUtilitySpace.getDomain().getIssues();
//        createFrequencyTable();
//    }
//
//    private void createFrequencyTable() {
//        for (Issue issue : issues) {
//            int issueNumber = issue.getNumber();
//            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
//            HashMap<String, Integer> issuesMap = new HashMap<>();
//            HashMap<String, Double> issuesMapRate = new HashMap<>();
//
//            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
//                issuesMap.put(valueDiscrete.getValue(), 0);
//                issuesMapRate.put(valueDiscrete.getValue(), 0.0);
//            }
//            frequencyTable.put(issueNumber, issuesMap);
//            frequencyRateTable.put(issueNumber, issuesMapRate);
//        }
//    }
//
//    public void calculateJonnyBlack(Bid receivedBid) {
//        for (Issue issue : receivedBid.getIssues()) {
//            Integer currentCount;
//            String valueName = receivedBid.getValue(issue).toString();
//            currentCount = frequencyTable.get(issue.getNumber()).get(valueName);
//            frequencyTable.get(issue.getNumber()).put(valueName, currentCount + 1);
//        }
////        predictValuation(receivedBid);
//    }
//
//    public double predictValuation(Bid bid) {
//        double[] issueWeightList = new double[frequencyRateTable.size()];
//        double[] issueWeightListHat = new double[frequencyRateTable.size()];
//
//        for (int issueIndex = 1; issueIndex <= frequencyTable.size(); issueIndex++) {
//            HashMap<String, Integer> map = frequencyTable.get(issueIndex);
//            int sumFrequency;
//            sumFrequency =
//                    (int) map.values().stream().collect(Collectors.summarizingInt(x -> x.intValue())).getSum();
//
//            int issueSize = map.size();
//            double[] singleWeight = new double[issueSize];
//            int i = 0;
//            for (String name : map.keySet()) {
//                int rank = frequencyTable.get(issueIndex).size() + 1;
//                for (Integer value : map.values()) {
//                    if (map.get(name) >= value) {
//                        rank -= 1;
//                    }
//                }
//                double calculatedValue = (double) (issueSize - rank + 1) / issueSize;
//                singleWeight[i] = Math.pow((((double) map.get(name) / (double) sumFrequency)), 2);
//                frequencyRateTable.get(issueIndex).put(name, calculatedValue);
//                i++;
//            }
//            issueWeightListHat[issueIndex - 1] = Arrays.stream(singleWeight).sum();
//        }
//
//        double sumWeightHat = Arrays.stream(issueWeightListHat).sum();
//        for (int index = 0; index < frequencyTable.size(); index++) {
//            issueWeightList[index] = issueWeightListHat[index] / sumWeightHat;
//        }
//
//        double predictedValue = 0.0;
//        for (Issue issue : bid.getIssues()) {
//            int number = issue.getNumber();
//            String valueName = bid.getValue(issue).toString();
//            predictedValue += issueWeightList[number - 1] * frequencyRateTable.get(number).get(valueName);
//        }
//
//        return predictedValue;
//    }
//
//    public double getUtility(Bid bid) {
//        //TODO Here don't need to change weight, just need to predict the Bid
//
//        double[] issueWeightList = new double[frequencyRateTable.size()];
//        double[] issueWeightListHat = new double[frequencyRateTable.size()];
//
//        for (int issueIndex = 1; issueIndex <= frequencyTable.size(); issueIndex++) {
//            HashMap<String, Integer> map = frequencyTable.get(issueIndex);
//            int sumFrequency;
//            sumFrequency =
//                    (int) map.values().stream().collect(Collectors.summarizingInt(x -> x.intValue())).getSum();
//
//            int issueSize = map.size();
//            double[] singleWeight = new double[issueSize];
//            int i = 0;
//            for (String name : map.keySet()) {
//                int rank = frequencyTable.get(issueIndex).size() + 1;
//                for (Integer value : map.values()) {
//                    if (map.get(name) >= value) {
//                        rank -= 1;
//                    }
//                }
//                double calculatedValue = (double) (issueSize - rank + 1) / issueSize;
//                singleWeight[i] = Math.pow((((double) map.get(name) / (double) sumFrequency)), 2);
//                frequencyRateTable.get(issueIndex).put(name, calculatedValue);
//                i++;
//            }
//            issueWeightListHat[issueIndex - 1] = Arrays.stream(singleWeight).sum();
//        }
//
//        double sumWeightHat = Arrays.stream(issueWeightListHat).sum();
//        for (int index = 0; index < frequencyTable.size(); index++) {
//            issueWeightList[index] = issueWeightListHat[index] / sumWeightHat;
//        }
//
//        double predictedValue = 0.0;
//        for (Issue issue : bid.getIssues()) {
//            int number = issue.getNumber();
//            String valueName = bid.getValue(issue).toString();
//            predictedValue += issueWeightList[number - 1] * frequencyRateTable.get(number).get(valueName);
//        }
//        return predictedValue;
//    }
//}

class JohnnyBlackComponentModel implements Serializable {

    private List<Issue> domainIssues;
    private List<Map<String, Integer>> issueValueFrequency = new ArrayList<>();
    private int totalNumberOfBids;
    private int issueCount;
    private double[] issueWeight;


    public void init(List<Issue> issues) {
        domainIssues = issues;
        issueCount = issues.size();
        issueWeight = new double[issueCount];
        for (int i = 0; i < issueCount; i++) {
            issueWeight[i] = 0;

            int numberOfValues = ((IssueDiscrete) domainIssues.get(i)).getNumberOfValues();
            Map<String, Integer> x = new HashMap<>();
            for (int j = 0; j < numberOfValues; j++) {
                String s = ((IssueDiscrete) domainIssues.get(i)).getValue(j).toString();
                x.put(s, 0);
            }
            issueValueFrequency.add(x);
        }
    }

    public void updateModel(Bid bid, int numberOfBids) {
        if (bid == null) return;
        totalNumberOfBids = numberOfBids;
        for (int i = 0; i < domainIssues.size(); i++) {
            String key = bid.getValue(i + 1).toString();
            Integer currentValue = issueValueFrequency.get(i).get(key);
            issueValueFrequency.get(i).put(key, ++currentValue);
        }
        updateIssueWeight();
    }

    public void updateIssueWeight() {
        for (int i = 0; i < issueCount; i++) {
            Map<String, Integer> issue = issueValueFrequency.get(i);
            issueWeight[i] = JohnnyBlackCalculateWeight(issue);
        }
        weightStandard();
    }

    public double getUtility(Bid bid) {
        double utility = 0;
        for (int i = 0; i < issueCount; i++) {
            String value = bid.getValue(i+1).toString();
            Map<String, Integer> issue = issueValueFrequency.get(i);
            Map<String, Double> ValueItem = JohnnyBlackItemValue(issue);
            utility += ValueItem.get(value) * issueWeight[i];
        }
        return utility;
    }

    private double JohnnyBlackCalculateWeight(Map<String, Integer> issue) {
        double sum = 0;
        for (Integer i : issue.values())
            sum += i;
        double out = 0;
        for (Integer i : issue.values())
            out += Math.pow(i / sum, 2);
        return out;
    }

    private Map<String, Double> JohnnyBlackItemValue(Map<String, Integer> issue) {
        ArrayList<Integer> list = new ArrayList<>(issue.values());
        Set<Integer> n = new HashSet<>(list);
        int size = issue.size();
        ArrayList<Integer> numbers = new ArrayList<>(n);
        Collections.sort(numbers);
        Collections.reverse(numbers);

        Map<String, Double> outcomes = new HashMap<>();
        for (Map.Entry entry : issue.entrySet()) {
            String k = (String) entry.getKey();
            Integer v = (Integer) entry.getValue();
            int rank = numbers.indexOf(v) + 1;

            double value = ((double) size - (double) rank + 1.0) / (double) size;
            outcomes.put(k, value);
        }
        return outcomes;
    }

    private void weightStandard() {
        double totalSum = Arrays.stream(issueWeight).sum();
        for (int i = 0; i < issueWeight.length; i++) {
            issueWeight[i] = issueWeight[i] / totalSum;
        }
    }
}
