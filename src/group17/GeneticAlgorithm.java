package group17;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

import java.util.*;

public class GeneticAlgorithm {
    private final int population = 200;
    private final int maxIteration = 1000;
    private final double rateOfMutation = 0.08;
    private final int numSelectedBids = 150;

    private final UserModel userModel;
    private final List<AbstractUtilitySpace> populationList = new ArrayList<>();

    public GeneticAlgorithm(UserModel userModel) {
        this.userModel = userModel;
        System.out.println("best unit is: "+mainFunction());
    }

    public AbstractUtilitySpace mainFunction() {
        System.out.println("----------GeneticAlgorithm------------");
        InitPopulation();       //创建population个 个体
        AbstractUtilitySpace bestUnit = startIteration();       //获得每个utility space的损失

        return bestUnit;
    }

    private List<AbstractUtilitySpace> chooseGoodPopulation(List<Double> lossList) {
        List<AbstractUtilitySpace> chosenPopulation = new ArrayList<>();
        List<Double> copyLossList = new ArrayList<>(lossList);

        Random random = new Random();
        double deletedNumber = -12345.6;

        for (int i=0; i < (population / 4); i++) {
            if (i > (population / 4) -3) {
                while (true) {
                    int randomNumber = random.nextInt(population-1);
                    if (copyLossList.get(randomNumber) != deletedNumber) {
                        chosenPopulation.add(populationList.get(randomNumber));
                        break;
                    }
                }

            }else {
                double maxUtility = Collections.max(copyLossList);
                int maxIndex = copyLossList.indexOf(maxUtility);
                chosenPopulation.add(populationList.get(maxIndex));
                copyLossList.set(maxIndex, deletedNumber);
            }
        }

        return chosenPopulation;
    }

    private AbstractUtilitySpace startIteration() {
        Random random = new Random();
        List<AbstractUtilitySpace> totalFinalPopulation = new ArrayList<>();
        List<Double> scoreList = new ArrayList<>();

        for (int i=0; i < maxIteration; i++) {
            List<AbstractUtilitySpace> selectedPopulation;
            List<Double> lossList = new ArrayList<>();
            for (int p=0; p < population; p++) {
                lossList.add(calculateUtilityScore(populationList.get(p)));
            }

            selectedPopulation = chooseGoodPopulation(lossList);

            for (int n=0; n < selectedPopulation.size(); n++) {
                totalFinalPopulation.add(selectedPopulation.get(n));
                scoreList.add(calculateUtilityScore(selectedPopulation.get(n)));
            }

            for (int j=0; j < selectedPopulation.size() / 2; j++) {
                while (true) {
                    int randomFatherNumber = random.nextInt(selectedPopulation.size() - 1);
                    int randomMotherNumber = random.nextInt(selectedPopulation.size() - 1);
                    if (randomFatherNumber != randomMotherNumber) {
                        AdditiveUtilitySpace father = (AdditiveUtilitySpace) selectedPopulation.get(randomFatherNumber);
                        AdditiveUtilitySpace mother = (AdditiveUtilitySpace) selectedPopulation.get(randomMotherNumber);
                        AbstractUtilitySpace son = crossover(father, mother);
                        totalFinalPopulation.add(son);
                        scoreList.add(calculateUtilityScore(son));
                        break;
                    }
                }
            }
        }
        double bestScore = Collections.max(scoreList);
        int index = scoreList.indexOf(bestScore);

        return totalFinalPopulation.get(index);
    }

    private AbstractUtilitySpace crossover(AdditiveUtilitySpace father, AdditiveUtilitySpace mother) {
        Random random = new Random();
        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(userModel.getDomain());
        List<IssueDiscrete> issues = additiveUtilitySpaceFactory.getIssues();

        for (IssueDiscrete issue: issues) {
            int randomIssueNumber = random.nextInt(2);
            if (randomIssueNumber == 0) {
                if (random.nextDouble() <= rateOfMutation) {
                    additiveUtilitySpaceFactory.setWeight(issue, random.nextDouble() + 0.01);
                }else {
                    additiveUtilitySpaceFactory.setWeight(issue, father.getWeight(issue));
                }
            }else {
                if (random.nextDouble() <= rateOfMutation) {
                    additiveUtilitySpaceFactory.setWeight(issue, random.nextDouble() + 0.01);
                }else {
                    additiveUtilitySpaceFactory.setWeight(issue, mother.getWeight(issue));
                }
            }
            for (ValueDiscrete value: issue.getValues()) {
                double fatherValueWeight = ((EvaluatorDiscrete) father.getEvaluator(issue)).getDoubleValue(value);
                double motherValueWeight = ((EvaluatorDiscrete) mother.getEvaluator(issue)).getDoubleValue(value);
                int randomValueNumber = random.nextInt(2);
                if (randomValueNumber == 0) {
                    if (random.nextDouble() <= rateOfMutation) {
                        additiveUtilitySpaceFactory.setUtility(issue, value, random.nextDouble() + 0.01);
                    }else {
                        additiveUtilitySpaceFactory.setUtility(issue, value, fatherValueWeight);
                    }
                }else {
                    if (random.nextDouble() <= rateOfMutation) {
                        additiveUtilitySpaceFactory.setUtility(issue, value, random.nextDouble() + 0.01);
                    }else {
                        additiveUtilitySpaceFactory.setUtility(issue, value, motherValueWeight);
                    }
                }
            }
        }
        additiveUtilitySpaceFactory.normalizeWeights();

        return additiveUtilitySpaceFactory.getUtilitySpace();
    }

    private double calculateUtilityScore(AbstractUtilitySpace abstractUtilitySpace) {
        double totalLoss = 0;
        double totalExp = 0;

        List<Double> expList = new ArrayList<>();
        List<Bid> bidList = userModel.getBidRanking().getBidOrder();
        HashMap<Integer, Double> selectedUtilityMap = new HashMap<>();

        if (bidList.size() > numSelectedBids) {
            int start = bidList.size() - numSelectedBids;
            int end = bidList.size();
            bidList = bidList.subList(start, end);
        }

        int order = 0;
        for (Bid bid : bidList) {
            selectedUtilityMap.put(order, abstractUtilitySpace.getUtility(bid));
            order++;
        }

        List<Map.Entry<Integer, Double>> mapList = new ArrayList<Map.Entry<Integer, Double>>(selectedUtilityMap.entrySet());
        Collections.sort(mapList, new Comparator<Map.Entry<Integer, Double>>() {
            @Override
            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

//        for (int i =0; i < mapList.size(); i++) {
//            double exp = Math.exp(mapList.get(i).getKey());
//            totalExp += exp;
//            expList.add(exp);
//        }

        for (int i =0; i < mapList.size(); i++) {
            int distance = Math.abs(mapList.get(i).getKey() - i);
            totalLoss += Math.pow(distance, 2);
//            double softMax = expList.get(i) / totalExp;
//            double loss = -(i*Math.log(softMax)/1000);
//            totalLoss += loss;
        }

        double x= totalLoss / (Math.pow(mapList.size(), 3));

        //total distance 越小， return数越大
        return -15 * Math.log(x + 0.000001f);
    }

    private void InitPopulation() {
        for (int i=0; i <= population; i++) {
            populationList.add(randomUnitGenerator());
        }
    }

    private AbstractUtilitySpace randomUnitGenerator() {
        List<Issue> issues;
        Random random = new Random();

        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(userModel.getDomain());
        issues = additiveUtilitySpaceFactory.getDomain().getIssues();

        for (Issue issue : issues) {
            additiveUtilitySpaceFactory.setWeight(issue, random.nextDouble());
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            for (ValueDiscrete value : issueDiscrete.getValues()) {
                additiveUtilitySpaceFactory.setUtility(issue, value, random.nextDouble());
            }
        }
        additiveUtilitySpaceFactory.normalizeWeights();
        return additiveUtilitySpaceFactory.getUtilitySpace();
    }
}
