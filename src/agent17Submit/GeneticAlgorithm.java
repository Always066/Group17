package agent17Submit;

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
    private final int population = 4000;
    private final int maxIteration = 200;
    private final double rateOfMutation = 0.001;
    private final int numSelectedBids = 150;
    int tolerant = 20;
    int reformNumber = 5;
    boolean switchTolerant = true;


    private final UserModel userModel;
    private List<AbstractUtilitySpace> populationList = new ArrayList<>();

    public GeneticAlgorithm(UserModel userModel) {
        this.userModel = userModel;
    }

    public AbstractUtilitySpace mainFunction() {
        System.out.println("----------GeneticAlgorithm------------");
        InitPopulation();       //创建population个 个体
        AbstractUtilitySpace bestUnit = startIteration();       //获得每个utility space的损失

        System.out.println("best unit is: " + bestUnit);
        return bestUnit;
    }

    private List<AbstractUtilitySpace> chooseGoodPopulation(List<Double> lossList) {
        List<AbstractUtilitySpace> chosenPopulation = new ArrayList<>();
        List<Double> copyLossList = new ArrayList<>(lossList);
        List<Double> orderedList = new ArrayList<>(copyLossList);
        Collections.sort(orderedList);
        Collections.reverse(orderedList);
        int chosenNumber = 250; //选择多少种群

        Random random = new Random();
        double deletedNumber = -12345.6;

        for (int i = 0; i < chosenNumber; i++) {
            int index = copyLossList.indexOf(orderedList.get(i));
            if (i > chosenNumber / 4) {
                while (true) {
                    int randomNumber = random.nextInt((populationList.size()) - i - 1) + i;
                    if (copyLossList.get(randomNumber) != deletedNumber) {
                        chosenPopulation.add(populationList.get(randomNumber));
                        break;
                    }
                }
            } else {

                chosenPopulation.add(populationList.get(index));
            }
        }

        return chosenPopulation;
    }

    private AbstractUtilitySpace startIteration() {
        Random random = new Random();
        List<Double> finalScoreList = new ArrayList<>();
        double GeneuisFactor = 0.1;
        double pastBestScore = 0;
        int toleratedTimes = 0;
        int reformedTimes = 0;
        AbstractUtilitySpace BEST = populationList.get(0);

        for (int i = 0; i < maxIteration; i++) {
            double sumError = 0;
            List<Double> scoreList = new ArrayList<>();
            List<AbstractUtilitySpace> totalFinalPopulation = new ArrayList<>();

            List<AbstractUtilitySpace> selectedPopulation;
            List<Double> lossList = new ArrayList<>();
            for (int p = 0; p < populationList.size(); p++) {
                double loss = calculateUtilityScore(populationList.get(p));
                sumError += loss;
                lossList.add(loss);
            }
            GeneuisFactor = 0.1;
            if (switchTolerant) {
                if ((sumError / populationList.size()) - pastBestScore < 3) {
                    toleratedTimes++;
                    if (toleratedTimes > tolerant) {
                        if (reformedTimes < reformNumber) {
                            GeneuisFactor = 0.5;
                            reformedTimes++;
                            toleratedTimes = 0;
                            System.out.println("开始变革");
                        } else {
                            break;
                        }
                    }
                }
            }

            pastBestScore = sumError / populationList.size();
            if (i % 10 == 0)
                System.out.println(i + " average error score: " + sumError / populationList.size());

            selectedPopulation = chooseGoodPopulation(lossList);

            for (AbstractUtilitySpace abstractUtilitySpace : selectedPopulation) {
                totalFinalPopulation.add(abstractUtilitySpace);
                scoreList.add(calculateUtilityScore(abstractUtilitySpace));
            }

            //分数排序
            List<Double> orderedScore = new ArrayList<>(scoreList);
            Collections.sort(orderedScore);
            Collections.reverse(orderedScore);


            for (int j = 0; j < selectedPopulation.size() / 4; j++) {
                while (true) {
                    int FatherIndex;
                    int MotherIndex;
                    // 90%的概率他们的父母都是最优秀的
                    if (new Random().nextDouble() > GeneuisFactor) {
                        List<Double> goodParents = orderedScore.subList(0, orderedScore.size() / 5);
                        int FatherNumber = new Random().nextInt(goodParents.size() - 1);
                        Double FatherScore = goodParents.get(FatherNumber);
                        FatherIndex = scoreList.indexOf(FatherScore);

                        int MotherNumber = new Random().nextInt(goodParents.size() - 1);
                        Double MotherScore = goodParents.get(MotherNumber);
                        MotherIndex = scoreList.indexOf(MotherScore);
                    } else {
                        FatherIndex = random.nextInt(selectedPopulation.size() - 1);
                        MotherIndex = random.nextInt(selectedPopulation.size() - 1);
                    }
                    if (FatherIndex != MotherIndex) {
                        AdditiveUtilitySpace father = (AdditiveUtilitySpace) selectedPopulation.get(FatherIndex);
                        AdditiveUtilitySpace mother = (AdditiveUtilitySpace) selectedPopulation.get(MotherIndex);
                        AbstractUtilitySpace son = crossover(father, mother);
                        totalFinalPopulation.add(son);
                        scoreList.add(calculateUtilityScore(son));
                        break;
                    }
                }
            }
            finalScoreList = scoreList;
            populationList = totalFinalPopulation;
            double bestScore = Collections.max(finalScoreList);
            int index = finalScoreList.indexOf(bestScore);

            if (calculateUtilityScore(populationList.get(index)) > calculateUtilityScore(BEST)) {
                BEST = populationList.get(index);
            }
        }

        return BEST;
    }

    private List<AdditiveUtilitySpace> getNiceParent(List<AbstractUtilitySpace> totalFinalPopulation, List<Double> scoreList) {
        List<AdditiveUtilitySpace> outputList = new ArrayList<>();


        return outputList;
    }

    private AbstractUtilitySpace crossover(AdditiveUtilitySpace father, AdditiveUtilitySpace mother) {
        Random random = new Random();

        double fatherUtility = calculateUtilityScore(father);
        double motherUtility = calculateUtilityScore(mother);

        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(userModel.getDomain());
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
                double fatherValueWeight = ((EvaluatorDiscrete) father.getEvaluator(issue)).getDoubleValue(value);
                double motherValueWeight = ((EvaluatorDiscrete) mother.getEvaluator(issue)).getDoubleValue(value);
                int randomValueNumber = random.nextInt(2);
                if (fatherUtility >= motherUtility) {
                    if (randomValueNumber <= 0.65) {
                        if (random.nextDouble() <= rateOfMutation) {
                            additiveUtilitySpaceFactory.setUtility(issue, value, random.nextDouble() + 0.01);
                        } else {
                            additiveUtilitySpaceFactory.setUtility(issue, value, fatherValueWeight);
                        }
                    } else {
                        if (random.nextDouble() <= rateOfMutation) {
                            additiveUtilitySpaceFactory.setUtility(issue, value, random.nextDouble() + 0.01);
                        } else {
                            additiveUtilitySpaceFactory.setUtility(issue, value, motherValueWeight);
                        }
                    }
                } else {
                    if (randomValueNumber > 0.65) {
                        if (random.nextDouble() <= rateOfMutation) {
                            additiveUtilitySpaceFactory.setUtility(issue, value, random.nextDouble() + 0.01);
                        } else {
                            additiveUtilitySpaceFactory.setUtility(issue, value, fatherValueWeight);
                        }
                    } else {
                        if (random.nextDouble() <= rateOfMutation) {
                            additiveUtilitySpaceFactory.setUtility(issue, value, random.nextDouble() + 0.01);
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

        List<Map.Entry<Integer, Double>> mapList = new ArrayList<Map.Entry<Integer, Double>>(selectedUtilityMap.entrySet());
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
        for (int i = 0; i < population; i++) {
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
