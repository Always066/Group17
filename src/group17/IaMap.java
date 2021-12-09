package group17;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.uncertainty.UserModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**整个类就是一个HashMap**/
public class IaMap extends HashMap<Issue, List<ValueNew>> {

    public int countBidNumber=0;    //用来计算对手出的bid的数量
    HashMap<Issue,Double> weightList=new HashMap<>();   //用来存放每个issue的权重

    /**1.整个类的构造函数，用于存放着当前运行下的整个table。可以理解为初始化table**/
    public IaMap(UserModel userModel){
        super();   //继承所有Hashmap的用法
        for(Issue issue:userModel.getDomain().getIssues()){          //遍历当前问题下的所有issue
            IssueDiscrete values=(IssueDiscrete) issue;            //将issue展开为每个value
            List<ValueNew> list =new ArrayList<>();   //每一个issue都要创建自己的一个List<ValueList>
            for(int i=0;i<values.getNumberOfValues();i++){    //因为这里的Value类型不能直接for each，只能用getNumberOfValues
                ValueNew temp=new ValueNew(values.getValue(i));   //对于每一个value类型，我们都转化为ValueNew类型
                list.add(temp);   //对于每一个value，我们都会将valueNew放进列表里
            }
            this.put(issue,list);
        }
    }

    /**2.计算jonnyBlack的方法**/
    public void JonnyBlack(Bid lastOffer){
        this.countBidNumber+=1;  //用来算现在有多少个bid的数据了

        //先遍历，打出个频数表
        for(Issue issue: lastOffer.getIssues()){
            int num=issue.getNumber();   //每一个issue我们都要将其转换为一个编号

            for(ValueNew valueNew:this.get(issue)){   //通过issue我们可以找到IaMap中的每一行
                if(valueNew.valueName.toString().equals(lastOffer.getValue(num).toString())){   //注意，每个bid都可以通过getValue(num)知道这个issue(issue对应的num)下到底是什么value
                    valueNew.count+=1;
                }

                //这里要赋值每一个valueNew对象一个totalOfOptions的值，用来计算在当前这个value下，有多少个options。
                IssueDiscrete issueDiscrete=(IssueDiscrete) issue;
                valueNew.totalOfOptions=issueDiscrete.getNumberOfValues(); //每个options的数量传进去
                valueNew.countBidNumber=this.countBidNumber;  //还要把这是第几个bid也传进去
            }
            Collections.sort(this.get(issue),this.get(issue).get(0));//每次对每一个list（this.get(issue)返回的是一个list），就对list进行降序。这里重写了排序方式，是根据count进行比较的

            //因为上面刚排序完，我们需要根据这个排序，重新赋予每一个valueNew一个rank值，其中频数越大,rank值小
            for(ValueNew valueNew:this.get(issue)){   //通过issue我们可以找到IaMap中的每一行
                valueNew.rank=this.get(issue).indexOf(valueNew)+1;
            }
        }

        //上面只是把表打好，但是一些计算还没有做好，所以现在要重新遍历一下每一个valueNew对象
        for(Issue issue:lastOffer.getIssues()){
            for(ValueNew valueNew:this.get(issue)){
                valueNew.compute();   //这一步主要是要把ValueNew内部的calculatedValue和weightUnnormalized计算好
            }
        }

        //做到这，该有的数据其实都有了，开始利用所有的数据，算权重了。但是这个循环不是为了算权重。而是为了归一化，求的分母totalWeight。（论文中第五个公式的分母）
        double totalWeight=0.0f;    //先初始化一个总的权重，用于后来的归一化。
        for(Issue issue: lastOffer.getIssues()){
            for(ValueNew valueNew:this.get(issue)){
                totalWeight+=valueNew.weightUnnormalized;
            }
        }

        //现在才开始算每一个issue的权重
        for(Issue issue:lastOffer.getIssues()){
            double issueWeightUnnormalized=0; //存放每个issue的权重的临时变量
            double maxWeightUnnormalized=0;
            for(ValueNew valueNew:this.get(issue)){
                if(valueNew.weightUnnormalized>maxWeightUnnormalized){
                    maxWeightUnnormalized=valueNew.weightUnnormalized;
                }
            }
            double issueWeight=maxWeightUnnormalized/totalWeight;
            this.weightList.put(issue,issueWeight);
        }

        //我们现在知道了每个issue的权重，现在需要来根据权重和每个value的evaluation来计算效用。
        //计算效用
        double utility=0.0f;   //先进行初始化
        for(Issue issue:lastOffer.getIssues()){
            int num=issue.getNumber();   //每一个issue我们都要将其转换为一个编号
            for(ValueNew valueNew:this.get(issue)){
                if(valueNew.valueName.toString().equals(lastOffer.getValue(num).toString())){   //注意，每个bid都可以通过getValue(num)知道这个issue(issue对应的num)下到底是什么value
                    utility+=weightList.get(issue)*valueNew.calculatedValue;
                    break;  //如果找到了，后面的valueNew就不需要找了。
                }
            }
        }

        System.out.println(countBidNumber+"对手效用是！！！！！！"+utility);
    }

    public double JBpredict(Bid lastOffer){
        //我们现在知道了每个issue的权重，现在需要来根据权重和每个value的evaluation来计算效用。
        //计算效用
        double utility=0.0f;   //先进行初始化

        for(Issue issue:lastOffer.getIssues()){
            int num=issue.getNumber();   //每一个issue我们都要将其转换为一个编号
            for(ValueNew valueNew:this.get(issue)){
                if(valueNew.valueName.toString().equals(lastOffer.getValue(num).toString())){   //注意，每个bid都可以通过getValue(num)知道这个issue(issue对应的num)下到底是什么value
                    utility+=weightList.get(issue)*valueNew.calculatedValue;
                    break;  //如果找到了，后面的valueNew就不需要找了。
                }
            }
        }
        return utility;
    }

}
