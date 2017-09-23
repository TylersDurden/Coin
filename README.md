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
Method for parsing dates from CSV: 
```java 
        TimeZone est = TimeZone.getTimeZone("EST");
        Calendar c = Calendar.getInstance(est, Locale.US);

        String cs = c.toString();
        String mo = cs.split(",MONTH=")[1].split(",")[0];
        String yr = cs.split(",YEAR=")[1].split(",")[0];
        String day = cs.split("DAY_OF_MONTH=")[1].split(",")[0];
 ```
