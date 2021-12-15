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
    private final int population = 2000;
    private final int maxIteration = 200;
    private final double rateOfMutation = 0.001;
    private final int numSelectedBids = 150;

    private final UserModel userModel;
    private List<AbstractUtilitySpace> populationList = new ArrayList<>();

    public GeneticAlgorithm(UserModel userModel) {
        this.userModel = userModel;
    }

    public AbstractUtilitySpace mainFunction() {
        System.out.println("----------GeneticAlgorithm------------");
        InitPopulation();       //创建population个 个体
        AbstractUtilitySpace bestUnit = startIteration();       //获得每个utility space的损失

        System.out.println("best unit is: "+bestUnit);
        return bestUnit;
    }

    private List<AbstractUtilitySpace> chooseGoodPopulation(List<Double> lossList) {
        List<AbstractUtilitySpace> chosenPopulation = new ArrayList<>();
        List<Double> copyLossList = new ArrayList<>(lossList);

        Random random = new Random();
        double deletedNumber = -12345.6;

        for (int i=0; i < 500; i++) {
            if (i > (500 / 4) - 10) {
                while (true) {
                    int randomNumber = random.nextInt((populationList.size())-1);
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
        List<Double> finalScoreList = new ArrayList<>();

        for (int i=0; i < maxIteration; i++) {
            double sumError = 0;
            List<Double> scoreList = new ArrayList<>();
            List<AbstractUtilitySpace> totalFinalPopulation = new ArrayList<>();

            List<AbstractUtilitySpace> selectedPopulation;
            List<Double> lossList = new ArrayList<>();
            for (int p=0; p < populationList.size(); p++) {
                double loss = calculateUtilityScore(populationList.get(p));
                sumError += loss;
                lossList.add(loss);
            }
            if (i%40==0)
                System.out.println(i+" average error score: "+ sumError / populationList.size());

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
            finalScoreList = scoreList;
            populationList = totalFinalPopulation;
        }
        double bestScore = Collections.max(finalScoreList);
        int index = finalScoreList.indexOf(bestScore);

        return populationList.get(index);
    }

    private AbstractUtilitySpace crossover(AdditiveUtilitySpace father, AdditiveUtilitySpace mother) {
        Random random = new Random();

        double fatherUtility =  calculateUtilityScore(father);
        double motherUtility = calculateUtilityScore(mother);

        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(userModel.getDomain());
        List<IssueDiscrete> issues = additiveUtilitySpaceFactory.getIssues();

        for (IssueDiscrete issue: issues) {
            double randomIssueNumber = random.nextDouble();
            if (fatherUtility >= motherUtility) {
                if (randomIssueNumber <= 0.65) {
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
            }else {
                if (randomIssueNumber > 0.65) {
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
            }
            for (ValueDiscrete value: issue.getValues()) {
                double fatherValueWeight = ((EvaluatorDiscrete) father.getEvaluator(issue)).getDoubleValue(value);
                double motherValueWeight = ((EvaluatorDiscrete) mother.getEvaluator(issue)).getDoubleValue(value);
                int randomValueNumber = random.nextInt(2);
                if (fatherUtility >= motherUtility) {
                    if (randomValueNumber <= 0.65) {
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
                } else {
                    if (randomValueNumber > 0.65) {
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
        }
        additiveUtilitySpaceFactory.normalizeWeights();

        return additiveUtilitySpaceFactory.getUtilitySpace();
    }

    private double calculateUtilityScore(AbstractUtilitySpace abstractUtilitySpace) {
        double totalLoss = 0;

        List<Bid> bidList = userModel.getBidRanking().getBidOrder();
        HashMap<Integer, Double> selectedUtilityMap = new HashMap<>();

        if (bidList.size() > numSelectedBids) {
            int start = bidList.size() - numSelectedBids;
            int end = bidList.size();
            bidList = bidList.subList(0, end);
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

        for (int i =0; i < mapList.size(); i++) {
            int distance = mapList.get(i).getKey() - i;
            totalLoss += Math.pow(distance, 2);
        }

        double x= totalLoss / (Math.pow(mapList.size(), 3));

        //total distance 越小， return数越大
        return -10 * Math.log(x + 0.00001f);
    }

    private void InitPopulation() {
        for (int i=0; i < population; i++) {
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
