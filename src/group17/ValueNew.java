package group17;

import genius.core.issue.Value;
import java.util.Comparator;

public class ValueNew implements Comparator<ValueNew> {
    public Value valueName;
    public int count=0;    //计数器，记录一共出现了多少次
    public int rank=0;     //根据options出现的次数，排一个rank，其中出现次数越多，rank越大
    public int totalOfOptions=0;  //记录当前options有多少个。也就是说一个value下有多少个options
    public int countBidNumber=0;  //记录对手一共出了多少次bid，也就是每个issue中的value总频数
    public double calculatedValue=0.0f; //论文上用来计算每个options的calculatedValue
    public double weightUnnormalized=0.0f; //在论文中是count/出现的总次数值

    public ValueNew(Value valueName) {
        this.valueName = valueName;
    }

    //我们需要根据count数目进行排序
    @Override
    public int compare(ValueNew o1, ValueNew o2) {
        if(o1.count < o2.count){
            return 1;
        }else{
            return -1;
        }
    }
    public void compute(){
        this.calculatedValue=((this.totalOfOptions-(double)this.rank+1)/this.totalOfOptions);
        double temp=((double) this.count/(double) this.countBidNumber);
        this.weightUnnormalized=Math.pow(temp,2);
    }
}
