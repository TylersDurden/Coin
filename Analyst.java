/* Analyst.java */
import java.nio.*;
import java.net.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.Date.*;
import java.util.TimeZone;
import java.util.Calendar.*;
import java.util.regex.*;
import java.sql.Time;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.net.InetAddress;

/**
 * Essentially taking the cleanest parts of btc.java and refining it into a better class
 */
public class Analyst {

    private static String              src        = "https://coinmarketcap.com/currencies/volume/24-hour/";
    private static String              src2       = "https://www.bfxdata.com/orderbooks/btcusd";
    private static String              src3       = "https://www.bitcoinwisdom.com/";

    public  static double              now;
    private static double              start;
    private static int                 selection;
    private static int                 time;

    //Market Data
    public static String[]             markets    = {"Bitstamp", "Bitfinex", "OKCoin", "Coinbase"};
    static String[]                    code       = new String[1000];
    public static double[]             market     = new double[4];                                          //instant market price
    public static double[][]           prices     = new double[4][100];                                     // store 100 price points for each market 
    static double[]                    slope      = new double[100];                                        //
    static double[]                    deriv      = new double[100];

    static double                      UpperLim;
    static double                      LowerLim;
    static int                         vectorPos;

    //Maps  
    public static Map<Integer, Double> Bitfinex   = new HashMap<Integer, Double>();
    public static Map<Integer, Double> Bitstamp   = new HashMap<Integer, Double>();
    public static Map<Integer, Double> OKCoin     = new HashMap<Integer, Double>();
    public static Map<Integer, Double> Coinbase   = new HashMap<Integer, Double>();
    //only do histogram for the selected market for now <Price,Frequency>
    public static Map<Double, Integer> Histo      = new HashMap<Double, Integer>();
    public static Vector<Double>       movement   = new Vector<>();
    public static Vector<Double>       derivative = new Vector<>();                                        // d/dt of movement 
    //Map the weights of large Bin prices to the upper/lower limits of histogram to 
    //get a feel of swing direction Map<Price,distUpper>
    public static Map<Double, Double>  fieldBull  = new HashMap<Double, Double>();
    public static Map<Double, Double>  fieldBear  = new HashMap<Double, Double>();

    //only measuring one market for now 
    int[]                              freq       = new int[slope.length];
    double[]                           avg        = new double[100];

    double[]                           bullA      = new double[100];
    double[]                           bearA      = new double[100];

    boolean                            refresh    = false;

    public Analyst() throws InterruptedException {
        start = System.currentTimeMillis() * .001;
        /* Hardcoding market selection to Bitfinex for now */
        selection = 1;
        datLog(TimeStamp());

        /*
         * Adds function and utility. 
         */
        //  bitWiz trader = new bitWiz();
        //System.out.println("\t\t- -- -HISTOGRAM_RUN- -- -");
        consoleLog("\t\t- -- -HISTOGRAM_RUN- -- -");
        while (true) {

            /* use info from bitWiz trader to track changes  */
            /* Connect to live data*/
            connect(src3);

            /*Parse Content */
            parse();

            /* Do histogram with 75 cent resolution */
            histogram(selection, 1);

            /* Get Time Elapsed */
            if(time%10==0){
                consoleLog("[" + getElapsedTime() + "s]");
             }

            time += 1;
            if (time == 1) {
                //populate pDat with sample range centered around current price 
                String printOut =
                                  "Histogram minimum at: $" + (prices[selection][0] - slope.length / 3) + " maximum at: $"
                                      + (prices[selection][0] + slope.length / 3);
                LowerLim = (prices[selection][0] - slope.length / 3);
                UpperLim = (prices[selection][0] + slope.length / 3);
                datLog(printOut);
            }

            checkSwing();
            
            //this boolean checks whether the histogram bins are getting heavy
            if(refresh==true){
             /*if that's true then we can probably begin to make approximations
              and also call the CrystalBall to make predictions. */
             
             CrystalBall prediction = new CrystalBall(
             deriv,avg,time, Histo,selection);
            
            }
            Thread.sleep(10000);
        }

    }

    public static String TimeStamp() {
        //set todays date, and initialize a calendar for calculations
        TimeZone est = TimeZone.getTimeZone("EST");
        Calendar c = Calendar.getInstance(est, Locale.US);

        String cs = c.toString();
        String mo = Integer.toString(Integer.parseInt(cs.split(",MONTH=")[1].split(",")[0]) + 1);//Indexed from 0!!
        String yr = cs.split(",YEAR=")[1].split(",")[0];
        String day = cs.split("DAY_OF_MONTH=")[1].split(",")[0];
        String hr = Integer.toString(Integer.parseInt(cs.split("HOUR=")[1].split(",")[0]) + 1);//toInt+1
        String min = cs.split("MINUTE=")[1].split(",")[0];
        String sec = cs.split("SECOND=")[1].split(",")[0];
        String mer = "";//(meridian) so AM or PM
        if (cs.split("AM_PM=")[1].split(",")[0].compareTo("1") == 0) {
            mer += "PM";
        } else {
            mer += "AM";
        }
        //hm so any of the three could be less than 10 but each might be different
        String dateHead = mo + "/" + day + "/" + yr + " " + hr + ":" + min + ":" + sec + " " + mer;
        /*
        String altHr; String altMin;String altS;
        if(hr<10){altHr = "0"+hr;}
        if(min<10){altmin ="0"+min;}
        if(sec<10){altS = "0"+sec;}
         */
        /* finish this later. hr/min/sec are strings so compareTo() for all 
        unless I come up with a better way to fix this, maybe a seperate class formatting
        timestamp
        if(hr<10||min<10||sec<10){
           if(hr<10&&min<&&sec<"10"){
               dateHead +=" "+altHr+":"+altmin+":"+altS; 
           }
           if(hr>10&&min<10&&sec<10){
               dateHead+=" "+hr+":"+altmin+":"+altS;
           }
           if(hr>10&&min>10&&sec<10){
               dateHead+=" "+hr+":"+min+":"+altS;
           }
           if(hr>10&&min>10&&sec>10){   
               dateHead+=" "+hr+":"+min+":"+sec+" "+mer;
           }
        

        }*/
        return dateHead;
    }

    /* Make the connection to the BTC Data */
    public void connect(String link) {
        String https = link;
        URL url;


        try {
            url = new URL(https);
            HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
            /**certificate info if needed */
            // getCert(con);
            /**Get content*/
            getContent(con);


        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.out.print("BAD ERL");
        } catch (IOException e) {
            e.printStackTrace();
        }
        // catch(URISyntaxException e){e.printStackTrace();
        // System.out.println();

    }

    static int updateTraderTime() { return time;}

    static double setTraderPrice(int market) {
        return prices[market][time];
    }

    /* Log method for calculations saved in math.txt */
    public static void datLog(String in) {
        BufferedWriter writer = null;
        try {
            Path p = Paths.get("./histogram.txt");
            File log = p.toFile();
            writer = new BufferedWriter(new FileWriter(log, true));
            writer.write(in + "\n");// <--logs it here
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /* Log method for calculations saved BtcJava */
    public static void consoleLog(String in) {
        BufferedWriter writer = null;
        try {
            Path p = Paths.get("./BtcJava.txt");
            File log = p.toFile();
            writer = new BufferedWriter(new FileWriter(log, true));
            writer.write(in + "\n");// <--logs it here
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /* Pull btc prices of major exchanges from the HTML */
    void parse() {
        //assigning market values based on HTML line index
        market = new double[4];
        market[0] = extract(code[19]); // Bitstamp
        market[1] = extract(code[25]); //Bitfinex
        market[2] = extract(code[28]); // okCoin
        market[3] = extract(code[213]); // Coinbase
        int time = (int)now / 10;
        //Now do it with HashMaps
        Bitstamp.put(time, extract(code[19]));
        Bitfinex.put(time, extract(code[25]));
        OKCoin.put(time, extract(code[28]));
        Coinbase.put(time, extract(code[213]));

        //Log data for the Market we chose to analyze
        // datLog(markets[selection]+" $"+market[selection]);
        //save prices of all Markets - Iterations is faster 
        int count = 0;
        for (double markt : market) {
            prices[count][time] = markt;
            count += 1;


        }
    }


    /* Pull the doubles (prices) from the strings */
    double extract(String str) {
        Double dig = 0.0;
        Matcher m = Pattern.compile("(?!=\\d\\.\\d\\.)([\\d.]+)").matcher(str);
        while (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return dig;
    }


    /* Grab content from HTTPS links */
    private void getContent(HttpsURLConnection con) {
        if (con != null) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;
                int nLns = 0;
                while ((line = br.readLine()) != null) {
                    //datLog(line);
                    code[nLns] = line;
                    nLns += 1;
                }
                // System.out.print(nLns+" lines read ");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /* Quick Method for getting program runtime */
    static double getElapsedTime() {
        now = System.currentTimeMillis() * .001 - start;
        return now;
    }

    /* Use for debugging the https connection */
    private void getCert(HttpsURLConnection con) {

        if (con != null) {
            //Try to get Server Certificates
            try {
                con.getResponseCode();
                con.getCipherSuite();

                Certificate[] certs = con.getServerCertificates();
                for (Certificate cert : certs) {
                    System.out.println("Certificate.Type: " + cert.getType());
                    System.out.println("Hash Code: " + cert.hashCode());
                    System.out.println("Public Key Algorithm: " + cert.getPublicKey().getAlgorithm());
                    System.out.println("Public Key Format: " + cert.getPublicKey().getFormat());
                }

            } catch (SSLPeerUnverifiedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /* Analyze price distribution as data is collected 
    |_____|_____|_____|_____|_____|
    |_____|_____|_____|_____|_____|
    |_____|_____|_____|_____|_____|
    |_____|_____|_____|_____|_____|
    Bin is $currentPrice +or- binDepth
     */
    //TODO: Instead read the current datLog file to make Histogram!!! Parse for $ 
    void histogram(int whichMarket, double binDepth) {
        //creating a histogram for [$price][frequency]
        //accumulates until array is full 
        int[][] histo = new int[100][100];
        double[] pDat = new double[100];//price domain
        double[] past = new double[100];

        double star = 0;

        for (int i = 0; i < pDat.length; i++) {
            pDat[i] = (prices[whichMarket][0] - (pDat.length / 3)) + i;

        }
        /*initialize histogram with the center on the initial price 
        the program starts at for the selected market */
        for (int i = 0; i < pDat.length; i++) {
            if (prices[whichMarket][0] == market[selection]) {
                // freq[i]+=1;
                //datLog(" $"+market[selection]+" : "+freq[i]);
            } else if (Math.abs(market[selection] - pDat[i]) <= binDepth) {
                freq[i] += 1;
                histo[time][i] = freq[i];
                Histo.put(pDat[i], freq[i]);
                //datLog(" $"+pDat[i]+" : "+freq[i]);
            }

        }
        String log = "";
        int binCount = 0;
        double delT = now / time;//time resolution of derivative Vector  
        for (Map.Entry<Double, Integer> entry : Histo.entrySet()) {
            movement.add((entry.getKey() - prices[whichMarket][0]));

            if (entry.getValue() > 1) {
                log += " $" + entry.getKey() + ":" + entry.getValue() + "\t";
                binCount += 1;
                fieldBull.put(entry.getKey(), UpperLim - entry.getKey());//+ means climbing,- is swing
                fieldBear.put(entry.getKey(), entry.getKey() - LowerLim); //+ means falling, - means swung
            }
            if (entry.getValue()> 8) {//organize the printout a bit 
                datLog(log + "\n");
                refresh = true; // trigger the CrystalBall 
            } else if (entry.getValue() > 10) {
                refresh = true;
                datLog("**CONSOLIDATING DATA ***\n$" +
                entry.getKey() + ":" + entry.getValue());
            }


        }
        /* Constructing a vector across price distribution, which can then be mapped
         * to prices to see how each bin correspinds to the weight of it's location
            compared to center. Also, center should be shifted to the maxFreq bin! 
           (this could be true for more than one value, in which case, average them)
           For now it will be compared to where price started (time scales are small)
         */

        double sum = 0;
        double[] slope = new double[100];

        for (int j = 0; j < movement.size(); j++) {
            sum += movement.get(j);

            if (movement.size() > 0) {
                avg[time] = sum / movement.size();
            }

        }

        double perc = (avg[time] / prices[whichMarket][0]) * 100;//percent 

        if (time > 0) {
            deriv[time] = avg[time] / time;
        }


        if (time > 0) {
           // System.out.print("Deriv: $" + deriv[time] +"/second [" + getElapsedTime() + " s]");
        }
       // System.out.print("MovingAvg: $" + avg[time]);
       // System.out.print(" " + perc + "%");
        consoleLog("["+now+"s] Deriv: $" + deriv[time] +
        "/second " + "MovingAvg: $" + avg[time] + "\n" + perc + "%");
        String dataDump =
                          "Time:" + now + "\tMoving Avg: $" + avg[time] + "\tDerivative: $" + deriv[time] + "\t" + perc + "% "
                              + TimeStamp() + "\n Watching " + markets[selection] + "\tInitial Price $" + prices[selection][0];

        if ((int)now % 10 == 0) {
            datLog(dataDump);
        }


    }
    
    
    

    /* Analyze current data, and check for current market tilt towards either 
     * Bear or Bull, depending on the weights of fieldBull/fieldBear, also use 
     * the Vector<Double> movement to possibly do trendFitting?*/
    public void checkSwing() {


        //fieldBull<price,distUpper> and fieldBear<price,distLower>
        //max diff -> hysteresis = ~ +/-$33/3

        /* Loop through all of prices and pDat, and try and map the movement<> entry
        values to bincount at the price. Knowing there was a instant of high or low 
        price affects moving avg, but if it was one off or brief it isn't as 
        descriptive or useful. Instead assign weights to these prices based on freq. */
        //slope[time] or avg[time] 

        double[] priceHistory = new double[100];
        double upperDist = 0;
        double lowerDist = 0;
        int index = 0;
        int index2 = 0;

        for (int i = 0; i < time; i++) {
            priceHistory[i] = prices[selection][i];
        }
        Arrays.sort(priceHistory);//use for mins and max during capture, compare with 33 

        if (priceHistory[priceHistory.length - 1] != prices[selection][0]) {
            // System.out.println(priceHistory[priceHistory.length-1]);//largest price not most current 
        }

        for (Map.Entry<Double, Double> entry : fieldBull.entrySet()) {
            upperDist = entry.getValue();//distance from upper limit
            //Now do a moving average on the bulls! 
            bullA[index] = entry.getKey();
            index += 1;
        }


        for (Map.Entry<Double, Double> entry : fieldBear.entrySet()) {
            lowerDist = entry.getValue();//distance from lower limit 
            bearA[index2] = entry.getKey();
            index2 += 1;
        }

        /* If these two numbers != then there is a trade imbalance b/w buyers and sellers
         * so this is a good time to start doing calculations */
        int sumA = 0;
        int sumB = 0;
        if (index != index2) {
            if (Math.abs(upperDist) < 30) { // moving avg is a better indicator 
                System.out.println(" Bull market " + upperDist + " from swing");
            }
            if (Math.abs(lowerDist) < 30) {
                System.out.println(" Bear Market " + lowerDist + " from swing");
            }


            //Now figure out averages of bears and bulls
            for (int i = 0; i < bearA.length; i++) {
                sumA += bearA[i];
            }
            for (int i = 0; i < bullA.length; i++) {
                sumB += bullA[i];
            }

            double bearAvg = sumA / bearA.length;
            double bullAvg = sumB / bullA.length;

            datLog("**Trade Imbalance** " + " BearsAvg: " + bearAvg + " BullsAvg: " + bullAvg + "\nBull Market $" + upperDist
                   + " from swing " + " Bear Market $" + lowerDist + " from swing");
        }

    }


    /**Main Method simply creates a new Analyst*/
    public static void main(String[] args) {
        try {
            new Analyst();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
/* Analyst.java */