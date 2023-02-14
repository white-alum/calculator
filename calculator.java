import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
/**
   Author: Jason 2023-02-12
*/
public class Calcul {
    // 累计前面计算值
    private BigDecimal preTotal;
    // 新的计算值
    private BigDecimal newNum;
    // 最近的一系列操作值
    private final List<BigDecimal> latestNumList = new ArrayList<>();
    // 最近的一系列操作
    private final List<String> latestOptList = new ArrayList<>();
    // 最近一系列操作的值的总值
    private final List<BigDecimal> latestTotalList = new ArrayList<>();
    // 当前操作符
    private String currentOperator;
    // undo/redo最近的操作索引
    private int latestOptIndex = -1;
    // 默认精度小数点后2位
    private final int scale = 2;
    // undo/redo有效索引最大值
    private int validIndexMax = -1;

    public void setNewNum(BigDecimal newNum) {
        if(preTotal == null){
            // 未计算过,累计总值为第一个输入值
            preTotal = newNum;
        }else{
            this.newNum = newNum;
        }
    }

    public void setCurrentOperator(String currentOperator) {
        this.currentOperator = currentOperator;
    }

    /**
     *  计算,相当于计算器的等于按钮
     */
    public void calculateResult(){
        preTotal = preTotal == null ? BigDecimal.ZERO : preTotal;
        if(currentOperator == null){
            System.out.println("请选择操作!");
        }
        if(newNum != null){
            // 新输入值, 累加计算
            BigDecimal ret = calcTwoNum(preTotal, newNum);
            if(this.latestOptIndex == -1){
                // 未处于redo/undo中间过程
                latestTotalList.add(preTotal);
                latestNumList.add(newNum);
                latestOptList.add(currentOperator);
            }else{
                // 当执行在redo/undo中间过程时,覆盖undo/redo操作记录,并记录有效索引最大值
                this.latestOptIndex++;
                this.validIndexMax = this.latestOptIndex;
                this.latestTotalList.set(this.latestOptIndex, ret);
                this.latestNumList.set(this.latestOptIndex-1, newNum);
                this.latestOptList.set(this.latestOptIndex-1, currentOperator);
            }
            preTotal = ret;
            currentOperator = null;
            newNum = null;
        }
    }

    /**
     * 撤回到上一步操作
     */
    public void undo(){
        if(preTotal != null && latestOptIndex == -1){
            // 未进行undo/redo操作,存储最后计算结果
            latestTotalList.add(preTotal);
            currentOperator = null;
            newNum = null;
        }

        if(latestTotalList.size() == 0){
            System.out.println("无操作!");
        }else if(latestTotalList.size() == 1){
            System.out.println("undo后值:0,"+"undo前值:"+preTotal);
            preTotal = BigDecimal.ZERO;
        } else {
            if(latestOptIndex == -1){
                latestOptIndex = latestOptList.size()-1;
            }else{
                if(latestOptIndex-1 < 0){
                    System.out.println("无法再undo!");
                    return;
                }
                latestOptIndex--;
            }
            cancelPreOperate(latestTotalList.get(latestOptIndex),latestOptList.get(latestOptIndex), latestNumList.get(latestOptIndex));
        }
    }

    /**
     *  依据撤回的操作结果进行重做
     */
    public void redo(){
        try{
            if(latestOptIndex > -1){
                if(latestOptIndex + 1 == latestTotalList.size() || latestOptIndex+1 == this.validIndexMax+1){
                    System.out.println("无法再redo!");
                    return;
                }
                latestOptIndex++;

                redoOperate(latestTotalList.get(latestOptIndex),latestOptList.get(latestOptIndex-1), latestNumList.get(latestOptIndex-1));
            }
        }catch (Exception e){
            System.out.println("redo异常,latestOptIndex:"+latestOptIndex);
        }
    }

    private void redoOperate(BigDecimal redoTotal, String redoOpt, BigDecimal redoNum) {
        System.out.println("redo后值:"+redoTotal+",redo前值:"+preTotal+",redo的操作:"+redoOpt+",redo操作的值:"+redoNum);
        preTotal = redoTotal;
        currentOperator = null;
        newNum = null;
    }

    private void cancelPreOperate(BigDecimal latestTotal, String latestOpt, BigDecimal latestNum) {
        System.out.println("undo后值:"+latestTotal+",undo前值:"+preTotal+",undo的操作:"+latestOpt+",undo操作的值:"+latestNum);
        preTotal = latestTotal;
        currentOperator = null;
        newNum = null;
    }

    /**
     * 进行累计计算
     * @param preTotal 前面已累计值
     * @param newNum 新输入值
     * @return 计算结果
     */
    private BigDecimal calcTwoNum(BigDecimal preTotal,  BigDecimal newNum) {
        BigDecimal ret = BigDecimal.ZERO;
        currentOperator = currentOperator == null ? "+" : currentOperator;
        switch (currentOperator){
            case "+":
                ret = preTotal.add(newNum);
                break;
            case "-":
                ret = preTotal.subtract(newNum).setScale(scale, RoundingMode.HALF_UP);
                break;
            case "*":
                ret = preTotal.multiply(newNum).setScale(scale, RoundingMode.HALF_UP);
                break;
            case "/":
                ret = preTotal.divide(newNum, RoundingMode.HALF_UP);
                break;
        }
        return ret;
    }

    /**
     * 显示出操作结果
     */
    public void display(){
        StringBuilder sb = new StringBuilder();
        if(preTotal != null){
            sb.append(preTotal.setScale(scale, RoundingMode.HALF_DOWN));
        }
        if(currentOperator != null){
            sb.append(currentOperator);
        }
        if(newNum != null){
            sb.append(newNum);
        }
        System.out.println(sb);
    }

    public static void main(String[] args) {
        Calcul calcul = new Calcul();
        calcul.setNewNum(new BigDecimal(6));
        calcul.setCurrentOperator("+");
        calcul.setNewNum(new BigDecimal(3));
        calcul.display();
        calcul.calculateResult();
        calcul.display();
        calcul.setCurrentOperator("*");
        calcul.setNewNum(new BigDecimal(5));
        calcul.display();
        calcul.calculateResult();
        calcul.display();
        calcul.undo();
        calcul.display();

        System.out.println("开始中断undo过程，并且附加额外计算:+10");
        calcul.setCurrentOperator("+");
        calcul.setNewNum(new BigDecimal(10));
        calcul.display();
        calcul.calculateResult();
        calcul.display();

        System.out.println("中断当前计算结束,重新进行undo/redo操作!");
        calcul.undo();
        calcul.display();
        calcul.undo();
        calcul.display();
        calcul.redo();
        calcul.display();
        calcul.redo();
        calcul.display();
        calcul.redo();
        calcul.display();
        calcul.redo();
        calcul.display();
    }
}
