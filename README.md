# Coin
First repository/ new to Github

Project for scraping web for bitcoin prices, and keeping track of price movements on 4 major exchanges. 
Price movements are then logged to a text file. This is definitely a work in progress! Using git as a backup. 

# btc.java
Class gathers up to date information of market price for bitcoin exchanges. Constructs a Vector of the rate of change of 
market value for a user specified market, in order to begin constructing a Momentum Oscillator (used for predictive analytics). 
Prices across markets, as well as explicit changes in price are then logged to files dat.txt and mathlog.txt respectively 

Market names : 
```java
static String[]             markets  = {"Bitsamp", "Bitfinex", "OKCoin", "Coinbase"};
``` 
Instance of Market Values: 
```java 
 static Map<Integer, Double> Bitfinex = new HashMap<Integer, Double>();
 static Map<Integer, Double> Bitstamp = new HashMap<Integer, Double>();
 static Map<Integer, Double> OKCoin   = new HashMap<Integer, Double>();
 static Map<Integer, Double> Coinbase = new HashMap<Integer, Double>();
 ```
Movement of markets stored in arrays: 
```java
  static double[][]           minus    = new double[4][100];
  static double[][]           plus     = new double[4][100];
  static double[][]           movmt    = new double[4][100];
  ```

# priceHistory.java
Takes a year worth of Coinbase market close prices and organizes them into a HashMap. This allows previous prices to be referenced
and paired with historical dates. The goal is for the historical price data to provide context to the calculations that are done, and will be done, in the btc.java program. 

Organizing Dates/Prices, allowing searches with either date or prices as Key : 
```java
    static Set<String>          dates   = new HashSet<>();               /
    static List<Double>         prices  = new ArrayList<>();
    static Map<Integer, Double> history = new HashMap<Integer, Double>();
    static Map<String, Double>  MAP     = new HashMap<String, Double>();
```
Creating a moving average, and Identifying upward/downward swings
```java
 static Vector <Double> moving = new Vector<>(100,1);
 
 /* Analyze moving Vector to determine swing state of market
    and attempt to predict potential price reversals */
    static void movingAvg(int mark){     
       
       /* 1- Find min and max of Vector moving
        * 2 - using getVecAvg to see what avg of vector is
        * 3 - Keep Track of when avg flips its sign 
        * 4 - If large reversal or significant event start over 
        */
       
      double max = Collections.max(moving);
      double min =  Collections.min(moving);
      double mavg=  getVecAvg(moving);
        if(mavg<0){
           swing[time] = "Down";
       }else{
           swing[time] = "Up";
       }
       //Choosing a trend of 5 same swing{time} in a row
       //reversal imminent if avg changes sign or by over 50%
       //clear Vector <double> moving after a reversal 
```
Gather Market Data & Store important values 
```java 
 /* Memory */
 void member(double[] newest) {

        double []   del  = new double[4];
        double []   movt =  new double[4];
        double diff = 0;
        for (int i = 0; i < 4; i++) {
            String line = "\n";
            if (time > 0) {
                if (newest[i] > history[i][time - 1]) {
                    del[i] = newest[i] - history[i][time - 1];
                    log(markets[i] + " + " + del[i]);
                    plus[i][time] = del[i];
                    line += Double.toString(del[i]);
                }
                if (newest[i] < history[i][time - 1]) {
                    del[i] = newest[i] - history[i][time - 1];
                    log("**[" + markets[i] + " - " + 
                    del[i] + "]**");
                    minus[i][time] = del[i];
                    line += Double.toString(del[i]);
                }
                diff =  Math.abs(plus[i][time]) - Math.abs(minus[i][time]);
                if(diff>1.00){
                    Top[i] = queryMarkets(markets[selection]);
                    mathLog(markets[i]+ " BULLS "+ diff+
                    " $"+Top[i]);
                    Top[i] = history[i][time];
                }
                if(diff<-1.00){
                    Low[i] = queryMarkets(markets[selection]);
                    mathLog(markets[i]+" BEARS "+diff+
                    " $"+Low[i]);    
                }
            }
            momentum[i][time]=del[i];
            history[i][time] = newest[i];
            moving.add(time,diff);
```

Averaging the moving Vector <double> 

 ```java 
 /* Average input Vector v */
 static double getVecAvg(Vector v){
        double sum=0;
        for(int i=0;i<v.size();i++){
            sum+= (double) v.get(i);
        } 
        sum = sum/(v.size()+1);
        return sum;
    }
```
Method for Generating Timestamp in log files : 
```java 
TimeZone est = TimeZone.getTimeZone("EST");
Calendar c = Calendar.getInstance(est, Locale.US);

String cs = c.toString();
String mo = cs.split(",MONTH=")[1].split(",")[0];
String yr = cs.split(",YEAR=")[1].split(",")[0];
String day = cs.split("DAY_OF_MONTH=")[1].split(",")[0];
String hour = cs.split("HOUR_OF_DAY=")[1].split(",")[0];
String min = cs.split("MINUTE=")[1].split(",")[0];
String dateHead = mo+"/"+day+"/"+yr+" "+hour+":"+min;
 ```
