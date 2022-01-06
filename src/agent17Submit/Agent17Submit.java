package agent17Submit;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;


import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.OutcomeSpace;
import genius.core.boaframework.SortedOutcomeSpace;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.BidRanking;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;

/**
 * A simple example agent that makes random bids above a minimum target utility.
 * f
 *
 * @author Tim Baarslag
 */
public class Agent17Submit extends AbstractNegotiationParty {
    private static double rankThreshold;
    private double MINIMUM_TARGET = 0.7;
    private int round = 0;
    private Bid lastOffer;
    private GeneticAlgorithm geneticAlgorithm;
    private MyJonnyBlack songJonnyBlack;
    private JhonnyBlackModel jhonnyBlackModel;
    NegotiationInfo info;
    double whenBegin;
    SortedOutcomeSpace outcomeSpace;

    /**
     * Initializes a new instance of the agent.
     */
    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        this.info = info;
        jhonnyBlackModel = new JhonnyBlackModel(userModel.getDomain());

        geneticAlgorithm = new GeneticAlgorithm(userModel);
        songJonnyBlack = new MyJonnyBlack(utilitySpace);
        rankThreshold = 0;

        utilitySpace = (AdditiveUtilitySpace) geneticAlgorithm.mainFunction();
        System.out.println(utilitySpace);
        System.out.println("----------POSAlgorithm------------");


        whenBegin = 0.5; //0.5的时间以前都不接受

        outcomeSpace = new SortedOutcomeSpace((AdditiveUtilitySpace) utilitySpace);
    }


    /**
     * Makes a random offer above the minimum utility target Accepts everything
     * above the reservation value at the end of the negotiation; or breaks off
     * otherwise.
     */
    @Override
    public Action chooseAction(List<Class<? extends Action>> possibleActions) {
        // Check for acceptance if we have received an offer
        manageMiniTarget();
        if (lastOffer != null) {
            rankThreshold = Math.pow(timeline.getTime(), 4);
            rankThreshold = Math.min(rankThreshold, 0.3);
            System.out.println("Current threshold: " + rankThreshold);

            //当时间不到最后一刻
            if (timeline.getTime() >= 0.98) {
                rankThreshold = 0.5;
                return new Accept(getPartyId(), lastOffer);
            }
            //检查报价是否符合我们的预期
            else if (isRankAboveThreshold(lastOffer)) {
                return new Accept(getPartyId(), lastOffer);
            }
        }
        // 不符合我们的给出新的报价
        return new Offer(getPartyId(), getRandomBidAboveThreshold());
    }


    private void manageMiniTarget() {
        if (timeline.getTime() <= 0.5) {
            MINIMUM_TARGET = 0.85;
        } else if (timeline.getTime() > 0.5 && timeline.getTime() < 0.65) {
            MINIMUM_TARGET = 0.75;
        } else if (timeline.getTime() > 0.65 && timeline.getTime() <= 0.95) {
            MINIMUM_TARGET = 0.65;
        } else if (timeline.getTime() > 0.95) {
            MINIMUM_TARGET = 0.5;
        }
    }

    /**
     * Check if the rank of offer is above the threshold.
     * Elicit the offer if needed
     */
    private boolean isRankAboveThreshold(Bid bid) {
        // Check if bid is in current ranking
        BidRanking bidRanking = userModel.getBidRanking();
        List<Bid> bidOrder = bidRanking.getBidOrder();

        if (!bidOrder.contains(bid)) {
                // Elicit the bid rank from the user
                userModel = user.elicitRank(bid, userModel);
                bidRanking = userModel.getBidRanking();
            }
        int noRanks = bidRanking.getSize();
        System.out.println("总的报价列表有这么长:" + noRanks);
        int rank = bidRanking.indexOf(bid); // Highest index is ranked best
        System.out.println("这个报价排在:(排名越靠后，价值越高)" + rank);
        System.out.println("我们期望是top: " + Math.round(noRanks * rankThreshold));
        boolean result = (noRanks - rank) <= (noRanks * rankThreshold);
        if (1 - (utilitySpace.getUtility(bid) + (new Random().nextDouble() / 10) - 0.05) > MINIMUM_TARGET) {
            result = true;
        }
        System.out.println("Within threshold? " + result);
        if (timeline.getTime() < whenBegin)
            result = false;
        if (1 - utilitySpace.getUtility(bid) < MINIMUM_TARGET)
            result = false;
        return result;
    }


    private Bid getRandomBidAboveThreshold() {
        /*This function generate the bid with utility above the threshold*/
        BidRanking bidRanking = userModel.getBidRanking();
        int noRanks = bidRanking.getSize();
        int thresholdRanks = (int) (noRanks * rankThreshold);
        Random rand = new Random();
        int randRank = rand.nextInt(thresholdRanks + 1);
        double max_utility = 0;
        Bid output = bidRanking.getBidOrder().get(noRanks - randRank - 1);
        List<BidDetails> list = new OutcomeSpace((AdditiveUtilitySpace) utilitySpace).getAllOutcomes();
        Collections.shuffle(list);
        list = list.subList(0, 2000);
        for (BidDetails b : list) {
            Bid bid = b.getBid();
            if (1 - utilitySpace.getUtility(bid) > MINIMUM_TARGET) {
                double o1 = songJonnyBlack.calculateJonnyBlack(bid); //JhonnyBlack 建模得到的utility
                double o2 = jhonnyBlackModel.valuation_opponent(bid);
                double o3 = 1 - utilitySpace.getUtility(bid); //user获得的utility
                double d = Math.abs(o1 - o3);
                if (o1 + o3 > max_utility && d < 0.5 && o1 < 0.9) {
                    output = bid;
                    max_utility = o1 * o3;
                }
            }
        }
        Bid outputSocial = bidRanking.getBidOrder().get(noRanks - randRank - 1);
        if (utilitySpace.getUtility(output) < 1 - MINIMUM_TARGET)
            output = bidRanking.getBidOrder().get(noRanks - randRank - 1);
        System.out.println("我们给出报价，第" + round + "局，我们的效用值:" + (1 - utilitySpace.getUtility(output)) + ",对手的效用值:" + songJonnyBlack.calculateJonnyBlack(output));
        System.out.println("------------------------我们开始下一轮了-----------------------------");
        return output;
    }

    private Bid generateRandomBidAboveTarget() {
        Bid randomBid;
        double util;
        int i = 0;
        // try 100 times to find a bid under the target utility
        do {
            randomBid = generateRandomBid();
            util = utilitySpace.getUtility(randomBid);
        } while (util < MINIMUM_TARGET && i++ < 100);
        return randomBid;
    }

    private Bid getMaxUtilityBid() {
        try {
            return utilitySpace.getMaxUtilityBid();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Remembers the offers received by the opponent.
     */
    @Override
    public void receiveMessage(AgentID sender, Action action) {
        if (action instanceof Offer) {
            lastOffer = ((Offer) action).getBid();
            double o3 = getUtilitySpace().getUtility(lastOffer);
            double o4 = songJonnyBlack.calculateJonnyBlack(lastOffer);
            jhonnyBlackModel.update_model(lastOffer);
            System.out.println("第" + round + "局：" + "收到报价阶段，" + "song实现的JB:" + o4 + ",目前我的模型的utility: " + o3);
        }
        round++;
    }

    @Override
    public String getDescription() {
        return "Places random bids >= " + MINIMUM_TARGET;
    }

    /**
     * This stub can be expanded to deal with preference uncertainty in a more
     * sophisticated way than the default behavior.
     */
    @Override
    public AbstractUtilitySpace estimateUtilitySpace() {
        return super.estimateUtilitySpace();
    }

    private void Describe() {
        if (hasPreferenceUncertainty()) {
            System.out.println("Preference uncertainty is enabled.");
            BidRanking bidRanking = userModel.getBidRanking();
            System.out.println("The agent ID is:" + info.getAgentID());
            System.out.println("Total number of possible bids:" + userModel.getDomain().getNumberOfPossibleBids());
            System.out.println("The number of bids in the ranking is:" + bidRanking.getSize());
            System.out.println("The lowest bid is:" + bidRanking.getMinimalBid());
            System.out.println("The highest bid is:" + bidRanking.getMaximalBid());
            System.out.println("The elicitation costs are:" + user.getElicitationCost());
            List<Bid> bidList = bidRanking.getBidOrder();
            System.out.println("The 5th bid in the ranking is:" + bidList.get(4));
        }
    }

    private List<BidDetails> allBids() {

        OutcomeSpace outcomeSpace = new OutcomeSpace((AdditiveUtilitySpace) utilitySpace);
        List<BidDetails> totalOutcomes = outcomeSpace.getAllOutcomes();
        Collections.shuffle(totalOutcomes);
        return totalOutcomes;
    }
}
