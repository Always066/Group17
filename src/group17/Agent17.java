package group17;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

import PSO.MyPos;
import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Offer;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.UtilitySpace;

/**
 * A simple example agent that makes random bids above a minimum target utility.
 * f
 *
 * @author Tim Baarslag
 */
public class Agent17 extends AbstractNegotiationParty {
    private static double rankThreshold;
    private double MINIMUM_TARGET = 0.7;
    private Bid lastOffer;
    private JhonnyBlackModel jhonnyBlackModel;
    NegotiationInfo info;
    IaMap iaMap;
    MyPos pos;

    /**
     * Initializes a new instance of the agent.
     */
    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        this.info = info;
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        UserUtilitySpace utilitySpace = new UserUtilitySpace((AdditiveUtilitySpace) info.getUtilitySpace(),userModel);
        jhonnyBlackModel = new JhonnyBlackModel(utilitySpace);
        rankThreshold = 0;
        Describe();
        iaMap = new IaMap(userModel);
        double[][] a = utilitySpace.getFrequency();
        double[][] b = utilitySpace.getOption_value();
        pos = new MyPos(1000,userModel);
        pos.iterMultipleTimes(100);
    }


    /**
     * Makes a random offer above the minimum utility target Accepts everything
     * above the reservation value at the end of the negotiation; or breaks off
     * otherwise.
     */
    @Override
    public Action chooseAction(List<Class<? extends Action>> possibleActions) {
        // Check for acceptance if we have received an offer
        if (lastOffer != null) {

            rankThreshold = Math.pow(timeline.getTime(), 8);
            rankThreshold = Math.min(rankThreshold, 0.3);
            System.out.println("Current threshold: " + rankThreshold);

            //当时间不到最后一刻
            if (timeline.getTime() >= 0.99) {
                if (isRankAboveThreshold(lastOffer))
                    return new Accept(getPartyId(), lastOffer);
                else
                    return new EndNegotiation(getPartyId());
            }
            //检查报价是否符合我们的预期
            else if (isRankAboveThreshold(lastOffer)) {
                return new Accept(getPartyId(), lastOffer);
            }
        }
        // 不符合我们的给出新的报价
        return new Offer(getPartyId(), getRandomBidAboveThreshold());
    }

    void manageTime() {
        if (timeline.getTime() <= 0.5) {
            rankThreshold = 0.05;
        }
        if (timeline.getTime() > 0.5 && timeline.getTime() < 0.75) {
            rankThreshold = 0.15;
        }
        if (timeline.getTime() > 0.5 && timeline.getTime() < 0.95) {
            rankThreshold = 0.25;
        }
    }

    /**
     * Check if the rank of offer is above the threshold.
     * Elicit the offer if needed
     *


     */
    private boolean isRankAboveThreshold(Bid bid) {
        // Check if bid is in current ranking
        BidRanking bidRanking = userModel.getBidRanking();
        List<Bid> bidOrder = bidRanking.getBidOrder();
        if (bidOrder.contains(bid)) {
            // True if above rank, false otherwise
            int noRanks = bidRanking.getSize();
            System.out.println("Lovely this bid didn't contain \nNo. of ranks: " + noRanks);
            int rank = bidRanking.indexOf(bid); // Highest index is ranked best
            System.out.println("Rank of bid: " + rank);
            System.out.println("We expect rank: " + noRanks * rankThreshold);
            boolean result = (noRanks - rank) <= (noRanks * rankThreshold);
            System.out.println("Within threshold? " + result);
            return result;
        }

        // Elicit the bid rank from the user
        userModel = user.elicitRank(bid, userModel);
        bidRanking = userModel.getBidRanking();

        int noRanks = bidRanking.getSize();
        System.out.println("Unfortunately this bid didn't contain \nNo. of ranks: " + noRanks);
        int rank = bidRanking.indexOf(bid); // Highest index is ranked best
        System.out.println("Rank of bid: " + rank);
        System.out.println("We expect rank: " + noRanks * rankThreshold);
        boolean result = (noRanks - rank) <= (noRanks * rankThreshold);
        System.out.println("Within threshold? " + result);
        return result;
    }


    private Bid getRandomBidAboveThreshold() {
        /*This function generate the bid with utility above the threshold*/
        BidRanking bidRanking = userModel.getBidRanking();
        int noRanks = bidRanking.getSize();
        System.out.println("No ranks: " + noRanks);
        int thresholdRanks = (int) (noRanks * rankThreshold);
        System.out.println("Ranks within threshold: " + noRanks);
        Random rand = new Random();
        int randRank = rand.nextInt(thresholdRanks + 1);
        System.out.println("Random rank = " + randRank);
        double max_utility = 0;
        Bid output = bidRanking.getBidOrder().get(noRanks - randRank - 1);
        for (int i = 0; i < thresholdRanks; i++) {
            Bid bid = bidRanking.getBidOrder().get(noRanks - i - 1);
            double o1 = jhonnyBlackModel.valuation_opponent(bid); //JhonnyBlack 建模得到的utility
            double o3 = getUtility(bid); //user获得的utility
            if (o1 > max_utility) {
                output = bid;
                max_utility = o1;
            }
        }

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
            jhonnyBlackModel.update_model(lastOffer);
            iaMap.JonnyBlack(lastOffer);
            double o1 = jhonnyBlackModel.valuation_opponent(lastOffer);
            double o2 = iaMap.JBpredict(lastOffer);
            double o3 = getUtilitySpace().getUtility(lastOffer);
            System.out.println("收到报价阶段，我实现的JB" + o1 + ", 学长实现的JB：" + o2 + ",目前我的模型的utility: " + o3);

        }

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
}


