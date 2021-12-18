package group26;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Offer;
import genius.core.issue.*;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;

import java.util.HashMap;
import java.util.List;

public class Agent26 extends AbstractNegotiationParty {

    private Bid lastOffer;
    private AgentID opponentId;
    private Bid lastMyOffer;
    private static double MINIMUM_TARGET = 0.90;
    private static double MAX_CONCESSION = 0.14;
    HashMap<AgentID, HashMap<Issue, HashMap<Value, Integer>>> predictTable = new HashMap<>();

    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        PreferenceElicitation pe = new PreferenceElicitation(getDomain(), userModel.getBidRanking());
        pe.estimateUsingBidRanks(userModel.getBidRanking());
        utilitySpace = pe.getU();
        utilitySpace.setReservationValue(MINIMUM_TARGET - MAX_CONCESSION);
    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> possibleActions) {
        if (lastOffer != null) {
            if (getUtility(lastOffer) > (MINIMUM_TARGET - ((timeline.getTime() * MAX_CONCESSION * 0.175) - 0.035)) && (timeline.getTime() >= 0.20)) {
                System.out.println("very good =================" + getUtility(lastOffer));
                Accept offer = new Accept(getPartyId(), lastOffer);
            	lastMyOffer = offer.getBid();
            	return offer;
            }
            else if (timeline.getTime() < 0.20) {
                try {
                	Offer offer = new Offer(getPartyId(), utilitySpace.getMaxUtilityBid());
                	lastMyOffer = offer.getBid();
                	return offer;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (timeline.getTime() >= 0.98) {
                if (utilitySpace.getUtility(lastOffer) * predictOpponentWeight(opponentId, lastOffer) < 0.3) {
                    System.out.println("fuck off! :(, U: " + getUtility(lastOffer) + " Opp: " + predictOpponentWeight(opponentId, lastOffer));
                }
                else {
                    System.out.println("no choice! :(, U: " + getUtility(lastOffer) + " Opp: " + predictOpponentWeight(opponentId, lastOffer));
                    return new Accept(getPartyId(), lastOffer);
                }
            }
            else if (timeline.getTime() >= 0.90) {
                if (getUtility(lastOffer) >= MINIMUM_TARGET - 0.08 * ((timeline.getTime()) * 5 - 4)) {
                	Accept offer = new Accept(getPartyId(), lastOffer);
                	lastMyOffer = offer.getBid();
                	return offer;
                }
            }       
        }
        Offer offer = new Offer(getPartyId(), generateRandomBidAboveTarget(30));
    	lastMyOffer = offer.getBid();
    	return offer;
    }

    @Override
    public void receiveMessage(AgentID sender, Action act) {
        if (act instanceof Offer) {
            lastOffer = ((Offer) act).getBid();
            opponentId = act.getAgent();
            System.out.println("look it! , U: " + getUtility(lastOffer) + ", Opp: " + predictOpponentWeight(opponentId, lastOffer));
            if (!predictTable.containsKey(sender)) {
                HashMap<Issue, HashMap<Value, Integer>> temp = new HashMap<>();
                for (Issue issue : lastOffer.getIssues()) {
                    HashMap<Value, Integer> temp2 = new HashMap<>();
                    temp2.put(lastOffer.getValue(issue.getNumber()), 1);
                    temp.put(issue, temp2);
                }
                predictTable.put(sender, temp);
            } else {
                for (Issue issue : lastOffer.getIssues()) {
                    if (predictTable.get(sender).get(issue).containsKey(lastOffer.getValue(issue.getNumber()))) {
                        predictTable.get(sender).get(issue).put(lastOffer.getValue(issue.getNumber()), predictTable.get(sender).get(issue).get(lastOffer.getValue(issue.getNumber())) + 1);
                    } else {
                        predictTable.get(sender).get(issue).put(lastOffer.getValue(issue.getNumber()), 1);
                    }
                }
            }
        }
        super.receiveMessage(sender, act);
    }


    /**
     *
     * @param id opponent id
     * @param bid the bid you want to calculate
     * @return the predict Utility of this bid for opponent
     */
    private double predictOpponentWeight(AgentID id, Bid bid) {
        if (predictTable == null || !predictTable.containsKey(id)) {
            return -1;
        }
        double res = 0;
        Issue[] issues = new Issue[0];
        issues = predictTable.get(id).keySet().toArray(issues);
        double[][] info = new double[issues.length][];
        double sum = 0;
        for (int i = 0; i < issues.length; i++) {
            info[i] = calculateEachValue(predictTable.get(id).get(issues[i]), bid, issues[i].getNumber());
            sum += info[i][1];
        }
        for (double[] d : info) {
            res += d[0] * (d[2] / sum);
        }
        return res;
    }

    private double[] calculateEachValue(HashMap<Value, Integer> map, Bid bid, int issueNr) {
        Value[] values = new Value[0];
        values = map.keySet().toArray(values);
        double aim = 1;
        if (map.containsKey(bid.getValue(issueNr))) {
            aim = map.get(bid.getValue(issueNr));
        }
        int sum = 0;
        int rank = 1;
        for (Value v : values) {
            sum += map.get(v);
            // Can also be >=; if use >=, after the loop rank should minus 1
            if (map.get(v) > aim) {
                rank++;
            }
        }
//        System.out.println("len: " + values.length + " rank: " + rank + " sum: " + sum);
        double calculatedValue = ((double) (values.length - rank + 1)) / values.length;
//        System.out.println("weight: " + calculatedValue);
        double vSum = 0;
        for (Value v : values) {
            vSum += Math.pow(((double)map.get(v)) / sum, 2);
        }
        return new double[]{calculatedValue, vSum, Math.pow(aim / sum, 2)};
    }

    // Use the second approach in lab 3

    /**
     *
     * @param num : the amount of the above bid, at less be 1
     * @return : the bid which has the highest value for opponent
     */
    private Bid generateRandomBidAboveTarget(long num) {
        int count = 0;
        Bid randomBid;
        Bid res = lastMyOffer;
        int i = 0;
        double util;

        while (i < (getDomain().getNumberOfPossibleBids() / 5) && count < num){
            i++;
            randomBid = generateRandomBid();
            util = utilitySpace.getUtility(randomBid);
            if (util > MINIMUM_TARGET - (timeline.getTime() * MAX_CONCESSION)) {
                if (opponentId == null) {
                    return userModel.getBidRanking().getMaximalBid();
                }
                count++;
                if (res == null) {
                    res = randomBid;
                } else {
                    res = predictOpponentWeight(opponentId, randomBid) * utilitySpace.getUtility(randomBid)> predictOpponentWeight(opponentId, res) * utilitySpace.getUtility(res) ? randomBid : res;
                }
            }
        }
        return res == null ? generateRandomBid() : res;
    }


    @Override
    public String getDescription() {
        return "just for fun!";
    }
}
