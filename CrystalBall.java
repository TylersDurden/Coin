/* CrystalBall.java */
import java.io.*;
import java.util.*;

/* Class for making market predictions */
public class CrystalBall{
    
    String market;
    double initial_price;
    double current_price;
    
    double [] dprice    = new double [100];
    double [] movingAvg = new double [100];
    
    //variables for estimating future price. 
    double future5 =0;
    double future10 = 0;
    double future15=0;
    
    static double now; 
    static double firstTime;
    
    static Map <Double,Integer> data = new HashMap<>();
    static Map <Integer,Double> invData = new HashMap<>();
    static Vector <Double> prediction = new Vector<>();
    
    /* CrystalBall constructor */
    public CrystalBall(double[]deriv,double[]mvAvg,
                       int timeElapsed,Map <Double,Integer> history,
                       int Market){
        /** I want to do essentially everything here via static methods to check things
         *  and then make a prediction. 
         *
         */
        firstTime =  Analyst.now;
        market = Analyst.markets[Market];
        initial_price = Analyst.prices[Market][0];
        current_price = Analyst.market[Market];
        
        System.out.println("\t\t***STARTING CRYSTAL BALL***\n"+
                           market+" went from $"+initial_price+" to $"+
                           current_price+" in "+firstTime+" s");
        
       double dsum=0; int nelements=0;
       double davg=0;
       for(int i=0;i<deriv.length;i++){
           if(deriv[i]!=0){
               dsum+=deriv[i];
               nelements+=1;
               dprice[nelements] = dsum/nelements;
               }    
       }
       
        davg = dsum/nelements;
        double numRuns = firstTime/nelements;
        
        future10 = (600*davg) + current_price;
        future15 = (60*15*davg) + current_price;
        
        System.out.println("10 minute prediction: $"+future10 +
                          " 15 minute prediction: $"+future15);
                
    
        
    }
    
    public static void main(String[]args){
        
    }
}

/*CrystalBall.java*/