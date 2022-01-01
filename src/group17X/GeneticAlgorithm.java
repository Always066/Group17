package group17X;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

import java.util.*;

public class GeneticAlgorithm {
    private final int populationScale = 500;  //the scale of the population
    private final int maxIteration = 150; // maximum of iteration number
    private final double rateOfMutation = 0.08;
    private final int numSelectedBids = 150;
    private Random random;


    private final UserModel userModel;
    private List<AbstractUtilitySpace> populationList;

    public GeneticAlgorithm(NegotiationInfo info) {
        this.userModel = info.getUserModel();
        random = new Random();
        populationList = new ArrayList<AbstractUtilitySpace>();
    }

    public AbstractUtilitySpace mainFunction() {
        System.out.println("----------GeneticAlgorithm------------");
        InitPopulation();       //创建population个 个体
        AbstractUtilitySpace bestUnit = startIteration();       //获得每个utility space的损失

        System.out.println("best unit is: " + bestUnit);
        return bestUnit;
    }

    private List<AbstractUtilitySpace> chooseGoodPopulation(List<AbstractUtilitySpace> population
            , List<Double> lossList) {
        List<AbstractUtilitySpace> chosenPopulation = new ArrayList<>();
        int eliteNumber = 10;

        // Copy the score list
        List<Double> copyLossList = new ArrayList<>();
        for (Double aDouble : lossList) {
            copyLossList.add(aDouble);
        }

        for (int i = 0; i < eliteNumber; i++) {
            double maxFitness = Collections.max(copyLossList);
            int index = copyLossList.indexOf(maxFitness);
            chosenPopulation.add(population.get(index));

            double temp = Double.MIN_VALUE;
            copyLossList.set(index, temp);
        }

        double sumFitness = 0.0;
        for (int i = 0; i < eliteNumber; i++) {
            sumFitness += lossList.get(i);
        }

        // rotate wheel algorithm
        for (int i = 0; i < populationScale - eliteNumber; i++) {
            double randNum = random.nextDouble() * sumFitness;
            double sum = 0.0;
            for (AbstractUtilitySpace abstractUtilitySpace : population) {
                sum += lossList.get(i);
                if (sum > randNum) {
                    chosenPopulation.add(abstractUtilitySpace);
                    break;
                }
            }
        }
        return chosenPopulation;
    }

    private AbstractUtilitySpace startIteration() {
        List<Double> finalScoreList = new ArrayList<>();

        //Early stopping variables
        double GeneuisFactor = 0.1;
        AbstractUtilitySpace BEST;
        // end****setting

        for (int i = 0; i < maxIteration; i++) {
            List<Double> scoreList = new ArrayList<>(); // store the fitness scores

            for (AbstractUtilitySpace a : populationList) {
                scoreList.add(calculateUtilityScore(a));
            }
            populationList = chooseGoodPopulation(populationList, scoreList);
            for (int j = 0; j < populationList.size() * rateOfMutation; j++) {
                AdditiveUtilitySpace father =
                        (AdditiveUtilitySpace) populationList.get(random.nextInt(populationScale));
                AdditiveUtilitySpace mother =
                        (AdditiveUtilitySpace) populationList.get(random.nextInt(populationScale));
                AbstractUtilitySpace child = crossover(father, mother);
                populationList.add(child);
            }
            if (i % (int)(maxIteration/5) == 0)
                System.out.println("这是第" + i + "轮" + "，best score is:" + Collections.max(scoreList));
        }


        for (AbstractUtilitySpace i : populationList) {
            finalScoreList.add(calculateUtilityScore(i));
        }
        double bestScore = Collections.max(finalScoreList);
        int index = finalScoreList.indexOf(bestScore);

        BEST = populationList.get(index);
        return BEST;
    }

    private AbstractUtilitySpace crossover(AdditiveUtilitySpace father,
                                           AdditiveUtilitySpace mother) {
        Random random = new Random();

        double fatherUtility = calculateUtilityScore(father);
        double motherUtility = calculateUtilityScore(mother);

        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory =
                new AdditiveUtilitySpaceFactory(userModel.getDomain());
        List<IssueDiscrete> issues = additiveUtilitySpaceFactory.getIssues();

        for (IssueDiscrete issue : issues) {
            double randomIssueNumber = random.nextDouble();
            if (fatherUtility >= motherUtility) {
                if (randomIssueNumber <= 0.65) {
                    if (random.nextDouble() <= rateOfMutation) {
                        additiveUtilitySpaceFactory.setWeight(issue, random.nextDouble() + 0.01);
                    } else {
                        additiveUtilitySpaceFactory.setWeight(issue, father.getWeight(issue));
                    }
                } else {
                    if (random.nextDouble() <= rateOfMutation) {
                        additiveUtilitySpaceFactory.setWeight(issue, random.nextDouble() + 0.01);
                    } else {
                        additiveUtilitySpaceFactory.setWeight(issue, mother.getWeight(issue));
                    }
                }
            } else {
                if (randomIssueNumber > 0.65) {
                    if (random.nextDouble() <= rateOfMutation) {
                        additiveUtilitySpaceFactory.setWeight(issue, random.nextDouble() + 0.01);
                    } else {
                        additiveUtilitySpaceFactory.setWeight(issue, father.getWeight(issue));
                    }
                } else {
                    if (random.nextDouble() <= rateOfMutation) {
                        additiveUtilitySpaceFactory.setWeight(issue, random.nextDouble() + 0.01);
                    } else {
                        additiveUtilitySpaceFactory.setWeight(issue, mother.getWeight(issue));
                    }
                }
            }
            for (ValueDiscrete value : issue.getValues()) {
                double fatherValueWeight =
                        ((EvaluatorDiscrete) father.getEvaluator(issue)).getDoubleValue(value);
                double motherValueWeight =
                        ((EvaluatorDiscrete) mother.getEvaluator(issue)).getDoubleValue(value);
                int randomValueNumber = random.nextInt(2);
                if (fatherUtility >= motherUtility) {
                    if (randomValueNumber <= 0.65) {
                        if (random.nextDouble() <= rateOfMutation) {
                            additiveUtilitySpaceFactory.setUtility(issue, value,
                                    random.nextDouble() + 0.01);
                        } else {
                            additiveUtilitySpaceFactory.setUtility(issue, value, fatherValueWeight);
                        }
                    } else {
                        if (random.nextDouble() <= rateOfMutation) {
                            additiveUtilitySpaceFactory.setUtility(issue, value,
                                    random.nextDouble() + 0.01);
                        } else {
                            additiveUtilitySpaceFactory.setUtility(issue, value, motherValueWeight);
                        }
                    }
                } else {
                    if (randomValueNumber > 0.65) {
                        if (random.nextDouble() <= rateOfMutation) {
                            additiveUtilitySpaceFactory.setUtility(issue, value,
                                    random.nextDouble() + 0.01);
                        } else {
                            additiveUtilitySpaceFactory.setUtility(issue, value, fatherValueWeight);
                        }
                    } else {
                        if (random.nextDouble() <= rateOfMutation) {
                            additiveUtilitySpaceFactory.setUtility(issue, value,
                                    random.nextDouble() + 0.01);
                        } else {
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
            List<Bid> bidListNew = new ArrayList<>();
            double interval = (double) bidList.size() / (double) numSelectedBids;
            for (int i = 0; i < numSelectedBids; i++) {
                int index = (int) Math.round(interval * i);
                bidListNew.add(bidList.get(index));
            }
            bidList = bidListNew;
        }

        int order = 0;
        for (Bid bid : bidList) {
            selectedUtilityMap.put(order, abstractUtilitySpace.getUtility(bid));
            order++;
        }

        List<Map.Entry<Integer, Double>> mapList =
                new ArrayList<Map.Entry<Integer, Double>>(selectedUtilityMap.entrySet());
        Collections.sort(mapList, new Comparator<Map.Entry<Integer, Double>>() {
            @Override
            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        for (int i = 0; i < mapList.size(); i++) {
            int distance = mapList.get(i).getKey() - i;
            totalLoss += Math.pow(distance, 2);
        }

        double x = totalLoss / (Math.pow(mapList.size(), 3));

        //total distance 越小， return数越大
        return -10 * Math.log(x + 0.00001f);
    }

    private void InitPopulation() {
        for (int i = 0; i < populationScale; i++) {
            populationList.add(randomUnitGenerator());
        }
    }

    private AbstractUtilitySpace randomUnitGenerator() {
        List<Issue> issues;
        Random random = new Random();

        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory =
                new AdditiveUtilitySpaceFactory(userModel.getDomain());
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
