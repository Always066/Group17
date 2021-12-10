package group17;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;

import java.util.*;

public class MyJonnyBlack {
    private List<Issue> issues;
    private HashMap<Integer, HashMap<String, Integer>> frequencyTable = new HashMap<>();
    private HashMap<Integer, HashMap<String, Double>> frequencyRateTable = new HashMap<>();

    public MyJonnyBlack(AdditiveUtilitySpace additiveUtilitySpace) {
        issues = additiveUtilitySpace.getDomain().getIssues();
        createFrequencyTable();
    }

    private void createFrequencyTable() {
        for (Issue issue : issues) {
            int issueNumber = issue.getNumber();
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            HashMap<String, Integer> issuesMap = new HashMap<>();
            HashMap<String, Double> issuesMapRate = new HashMap<>();

            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
                issuesMap.put(valueDiscrete.getValue(), 0);
                issuesMapRate.put(valueDiscrete.getValue(), 0.0);
            }
            frequencyTable.put(issueNumber, issuesMap);
            frequencyRateTable.put(issueNumber, issuesMapRate);
        }
    }

    public double calculateJonnyBlack(Bid receivedBid) {
        for (Issue issue : receivedBid.getIssues()) {
            Integer currentCount;
            String valueName = receivedBid.getValue(issue).toString();
            currentCount = frequencyTable.get(issue.getNumber()).get(valueName);
            frequencyTable.get(issue.getNumber()).put(valueName, currentCount + 1);
        }
        double ValueJonnyBlack = predictValuation(receivedBid);
        return ValueJonnyBlack;
    }

    private double predictValuation(Bid bid) {
        double[] issueWeightList = new double[frequencyRateTable.size()];
        double[] issueWeightListHat = new double[frequencyRateTable.size()];

        for (int issueIndex = 1; issueIndex <= frequencyTable.size(); issueIndex++) {
            HashMap<String, Integer> map = frequencyTable.get(issueIndex);
            int sumFrequency = 0;
            for (int value : map.values()) {
                sumFrequency += value;
            }

            int issueSize = map.size();
            double[] singleWeight = new double[issueSize];
            int i = 0;
            for (String name : map.keySet()) {
                int rank = frequencyTable.get(issueIndex).size() + 1;
                for (Integer value : map.values()) {
                    if (map.get(name) >= value) {
                        rank -= 1;
                    }
                }
                double calculatedValue = (double) (issueSize - rank + 1) / issueSize;
                singleWeight[i] = Math.pow((((double) map.get(name) / (double)sumFrequency)), 2);
                frequencyRateTable.get(issueIndex).put(name, calculatedValue);
                i++;
            }
            issueWeightListHat[issueIndex-1] = Arrays.stream(singleWeight).sum();
        }

        double sumWeightHat = Arrays.stream(issueWeightListHat).sum();
        for (int index = 0; index < frequencyTable.size(); index++) {
            issueWeightList[index] = issueWeightListHat[index] / sumWeightHat;
        }

        double predictedValue = 0.0;
        for (Issue issue : bid.getIssues()) {
            int number = issue.getNumber();
            String valueName = bid.getValue(issue).toString();
            predictedValue += issueWeightList[number-1] * frequencyRateTable.get(number).get(valueName);
        }

        return predictedValue;
    }
}
