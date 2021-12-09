package group17;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Offer;
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
public class Agent17 extends AbstractNegotiationParty {
    private static double rankThreshold;
    private double MINIMUM_TARGET = 0.7;
    private Bid lastOffer;
    private JhonnyBlackModel jhonyBlackModel;
    NegotiationInfo info;
    IaMap iaMap;

    /**
     * Initializes a new instance of the agent.
     */
    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        this.info = info;
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        AbstractUtilitySpace utilitySpace = info.getUtilitySpace();
        jhonyBlackModel = new JhonnyBlackModel((AdditiveUtilitySpace) utilitySpace);
        rankThreshold = 0;
        Describe();
        iaMap = new IaMap(userModel);
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

            rankThreshold = Math.pow(timeline.getTime(), 10);

            System.out.println("Current threshold: " + rankThreshold);
            System.out.println();
            if (timeline.getTime() >= 0.99) {
                if (isRankAboveThreshold(lastOffer))
                    return new Accept(getPartyId(), lastOffer);
                else
                    return new EndNegotiation(getPartyId());
            } else if (isRankAboveThreshold(lastOffer)) {
                return new Accept(getPartyId(), lastOffer);
            }
        }

        // Otherwise, send out a random offer above the target utility

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
     * @param bid
     * @return
     */
    private boolean isRankAboveThreshold(Bid bid) {
        // Check if bid is in current ranking
        BidRanking bidRanking = userModel.getBidRanking();
        List<Bid> bidOrder = bidRanking.getBidOrder();
        if (bidOrder.contains(bid)) {
            // True if above rank, false otherwise
            int noRanks = bidRanking.getSize();
            System.out.println("No. of ranks: " + noRanks);
            int rank = bidRanking.indexOf(bid); // Highest index is ranked best
            System.out.println("Rank of bid: " + rank);
            boolean result = rank <= (noRanks * rankThreshold);
            System.out.println("Within threshold? " + result);
            return result;
        }

        // Elicit the bid rank from the user
        userModel = user.elicitRank(bid, userModel);
        bidRanking = userModel.getBidRanking();

        int noRanks = bidRanking.getSize();
        System.out.println("No. of ranks: " + noRanks);
        int rank = bidRanking.indexOf(bid); // Highest index is ranked best
        System.out.println("Rank of bid: " + rank);
        boolean result = rank <= (noRanks * rankThreshold);
        System.out.println("Within threshold? " + result);
        return result;
    }


    private Bid getRandomBidAboveThreshold() {
        BidRanking bidRanking = userModel.getBidRanking();
        int noRanks = bidRanking.getSize();
        System.out.println("No ranks: " + noRanks);
        int thresholdedRanks = (int) (noRanks * rankThreshold);
        System.out.println("Ranks within threshold: " + noRanks);
        Random rand = new Random();
        int randRank = rand.nextInt(thresholdedRanks + 1);
        System.out.println("Random rank = " + randRank);
        double max_utility = 0;
        Bid output = bidRanking.getBidOrder().get(noRanks - randRank - 1);
        for (int i = 0; i < thresholdedRanks && timeline.getTime() > 0.8; i++) {
            Bid bid = bidRanking.getBidOrder().get(i);
            double o1 = jhonyBlackModel.valuation_opponent(bid);
            double o2 = iaMap.JBpredict(bid);
            System.out.println("我实现的JB" + o1 + ", 学长实现的JB：" + o2);
            if (o1 * utilitySpace.getUtility(bid) > max_utility) {
                output = bid;
            }
        }
//        output = bidRanking.getBidOrder().get(0);
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
            jhonyBlackModel.update_model(lastOffer);
            iaMap.JonnyBlack(lastOffer);
            double o1 = jhonyBlackModel.valuation_opponent(lastOffer);
            double o2 = iaMap.JBpredict(lastOffer);
            System.out.println("我实现的JB" + o1 + ", 学长实现的JB：" + o2);

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

