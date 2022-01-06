package group17X;

import AgreeableAgent.Constants;
import AgreeableAgent.FrequencyBasedOpponentModel;
import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.OutcomeSpace;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.misc.Range;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.utility.AbstractUtilitySpace;

import java.util.List;

/*
 * 船新版本的Agent, 参考Agreeable Agent 的让步方法，结合我们的
 * */
public class Agent17X extends AbstractNegotiationParty {
    private int domainSize;
    private OutcomeSpace outcomeSpace = null;
    private AbstractUtilitySpace userUtilSpace;
    private double pMin;
    private double pMax;
    private Bid bestBid;
    private GeneticAlgorithm geneAlgorithm;
    private FrequencyBasedOpponentModel opponentModel;
    private double timeForUsingModel = 0.1;
    private int opponentBidCount = 0;
    private Bid lastReceivedOffer;


    @Override
    public void init(NegotiationInfo info) {
        super.init(info);

        outcomeSpace = new OutcomeSpace(utilitySpace);
        domainSize = outcomeSpace.getAllOutcomes().size();

        //if this is an uncertainty case, we could use User Model Algorithm
        if (hasPreferenceUncertainty() && ifUseUserModel()) {
            try { //except the failure of userModel Algorithm
                geneAlgorithm = new GeneticAlgorithm(info);
                this.userUtilSpace = geneAlgorithm.mainFunction();
            } catch (Exception e) {
                this.userUtilSpace = info.getUtilitySpace();
            }
        } else {
            this.userUtilSpace = info.getUtilitySpace();
        }

        try {
            bestBid = info.getUtilitySpace().getMaxUtilityBid();
            pMin = userUtilSpace.getUtility(userUtilSpace.getMinUtilityBid());
            pMax = userUtilSpace.getUtility(userUtilSpace.getMaxUtilityBid());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (isAllIssuesDiscrete()) {
            determineTimeForUsingModel();
            opponentModel = new FrequencyBasedOpponentModel();
            opponentModel.init(userUtilSpace.getDomain().getIssues());
        }
    }

    private boolean ifUseUserModel() {
        // 如果已知的offer数量小于百分之5，不对用户进行建模
        if (userModel.getBidRanking().getBidOrder().size() < domainSize / 100 * 5) {
            System.out.println("已知的bids数量太少，不进行建模");
            return false;
        }

        return true;
    }

    @Override
    public void receiveMessage(AgentID sender, Action lasterOpponentAction) {
        super.receiveMessage(sender, lasterOpponentAction);
        if (lasterOpponentAction instanceof Offer) {
            Bid bid = ((Offer) lasterOpponentAction).getBid();
            opponentBidCount++;
            opponentModel.updateModel(bid, opponentBidCount);
            lastReceivedOffer = bid;
        }
    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> possibleActions) {
        if (lastReceivedOffer != null) {
            try {
                Bid myBid = getNextBid();
                if (isAcceptable(getUtility(lastReceivedOffer), getUtility(myBid)))
                    return new Accept(getPartyId(), lastReceivedOffer);
                else
                    return new Offer(getPartyId(), myBid);
            } catch (Exception e) {
//                System.out.println(e);
                return new Offer(getPartyId(), bestBid);
            }
        } else {
            return new Offer(getPartyId(), bestBid);
        }
    }

    private boolean isAcceptable(double opponentUtility, double myBidUtilityBytime) {
        if (opponentUtility >= myBidUtilityBytime) {
            return true;
        }
        double time = getTimeLine().getTime();
        boolean out = time >= 0.99 && opponentUtility >= userUtilSpace.getReservationValue();
        return out;
    }


    @Override
    public String getDescription() {
        return "New version negotiation agent for group 17";
    }

    private boolean isAllIssuesDiscrete() {
        List<Issue> issues = userUtilSpace.getDomain().getIssues();
        for (Issue issue : issues) {
            if (!(issue instanceof IssueDiscrete)) {
                return false;
            }
        }
        return true;
    }

    private void determineTimeForUsingModel() {
        if (domainSize < Constants.smallDomainUpperBound)
            timeForUsingModel = Constants.timeForUsingModelForSmallDomain;
        if (domainSize >= Constants.smallDomainUpperBound &&
                domainSize < Constants.midDomainUpperBound)
            timeForUsingModel = Constants.timeForUsingModelForMidDomain;
        if (domainSize >= Constants.midDomainUpperBound)
            timeForUsingModel = Constants.timeForUsingModelForLargeDomain;
    }

    private Bid getNextBid() {
        double time = getTimeLine().getTime();
        double targetUtility = getUtilityByTime(time);
        if (targetUtility < Constants.minimumUtility)
            targetUtility = Constants.minimumUtility;
        if (isModelUsable()) {
            return provideBidByOpponentModel(targetUtility);
        } else {
            BidDetails bidDetails = outcomeSpace.getBidNearUtility(targetUtility);
            return bidDetails.getBid();
        }
    }

    private Bid provideBidByOpponentModel(double targetUtility) {

        double utilityThreshold = getExplorableNeighbourhood();
        Range range = new Range(targetUtility - utilityThreshold,
                targetUtility + utilityThreshold);
        List<BidDetails> bidsInRange = outcomeSpace.getBidsinRange(range);
        if (bidsInRange.size() == 1) {
            return bidsInRange.get(0).getBid();
        }
        int selectedBidIndex = getBidByRouletteWheel(bidsInRange);
        return bidsInRange.get(selectedBidIndex).getBid();
    }

    private int getBidByRouletteWheel(List<BidDetails> bidsInRange) {
        int size = bidsInRange.size();
        double[] sumOfTwoUtilitiesForBid = new double[size];
        double totalUtility = 0;
        for (int i = 0; i < size; i++) {
            BidDetails bidDetails = bidsInRange.get(i);
            double sum = opponentModel.getUtility(bidDetails.getBid());
            sumOfTwoUtilitiesForBid[i] = sum;
            totalUtility += sum;
        }
        double[] normalizedSumOfTwoUtiliesForBid = new double[size];
        for (int i = 0; i < size; i++) {
            normalizedSumOfTwoUtiliesForBid[i] = sumOfTwoUtilitiesForBid[i] / totalUtility;
        }
        double random = Math.random();
        double integrate = 0;
        int selectedBidIndex = size;
        for (int i = 0; i < size; i++) {
            integrate += normalizedSumOfTwoUtiliesForBid[i];
            if (integrate >= random) {
                selectedBidIndex = i;
                break;
            }
        }
        return selectedBidIndex;
    }

    private boolean isModelUsable() {
        double time = timeline.getTime();
        return time >= timeForUsingModel;
    }

    private double getUtilityByTime(double time) {
        if (time < Constants.timeToConcede) {
            return 1;
        } else {
            time = (time - Constants.timeToConcede) / (1 - Constants.timeToConcede);
            return pMin + (pMax - pMin) * (1 - f(time));
        }
    }

    public double f(double t) {
        if (Constants.concessionFactor == 0)
            return Constants.k;
        return Constants.k + (1 - Constants.k) * Math.pow(t, 1.0 / Constants.concessionFactor);
    }

    private double getExplorableNeighbourhood() {
        double time = getTimeLine().getTime();
        if (time < Constants.timeToConcede) {
            return 0;
        } else {
            return Constants.neigExplorationDisFactor
                    * (1 - (pMin + (pMax - pMin) * (1 - f(time))));
        }
    }

}
