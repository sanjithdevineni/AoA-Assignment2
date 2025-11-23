import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Random;
import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class DPAssign {
    static {
        System.setProperty("java.awt.headless", "true");
    }
    private static final int[][] P1_BATCH_SIZES = {
        {500, 500},
        {800, 1200},
        {1000, 1500},
        {1500, 2000},
        {2000, 2500}
    };
    private static final int[][] P2_SIZES = {
        {10, 10},
        {10, 100},
        {10, 1000},
        {100, 1000},
        {1000, 1000}
    };
    private static final Font TITLE_FONT = new Font("Monospaced", Font.BOLD, 20);
    private static final Font LABEL_FONT = new Font("Monospaced", Font.PLAIN, 14);

    public static void main(String[] args) {
        if (args.length == 0) {
            PrintWriter out = stdOutWriter();
            runP1(2000, 2500, 123L, out);
            out.println();
            runP2(0.4, 123L, out);
            out.flush();
            return;
        }

        String cmd = args[0];
        try {
            switch (cmd) {
                case "p1": {
                    int idx = 1;
                    int n = (idx < args.length) ? Integer.parseInt(args[idx++]) : 2000;
                    int m = (idx < args.length) ? Integer.parseInt(args[idx++]) : 2500;
                    long seed = (idx < args.length) ? Long.parseLong(args[idx++]) : 123L;
                    String outPath = (idx < args.length) ? args[idx] : null;
                    boolean fileOut = outPath != null;
                    PrintWriter out = fileOut ? fileWriter(outPath) : stdOutWriter();
                    runP1(n, m, seed, out);
                    finishWriter(out, fileOut);
                    break;
                }
                case "p1batch": {
                    int idx = 1;
                    long seed = (idx < args.length) ? Long.parseLong(args[idx++]) : 123L;
                    String outPath = (idx < args.length) ? args[idx] : null;
                    boolean fileOut = outPath != null;
                    PrintWriter out = fileOut ? fileWriter(outPath) : stdOutWriter();
                    runP1Batch(seed, out);
                    finishWriter(out, fileOut);
                    break;
                }
                case "p2": {
                    int idx = 1;
                    double pOne = (idx < args.length) ? Double.parseDouble(args[idx++]) : 0.4;
                    long seed = (idx < args.length) ? Long.parseLong(args[idx++]) : 123L;
                    String outPath = (idx < args.length) ? args[idx] : null;
                    boolean fileOut = outPath != null;
                    PrintWriter out = fileOut ? fileWriter(outPath) : stdOutWriter();
                    runP2(pOne, seed, out);
                    finishWriter(out, fileOut);
                    break;
                }
                case "p2png": {
                    int idx = 1;
                    double pOne = (idx < args.length) ? Double.parseDouble(args[idx++]) : 0.4;
                    long seed = (idx < args.length) ? Long.parseLong(args[idx++]) : 123L;
                    String prefix = (idx < args.length) ? args[idx] : "p2_plot";
                    PrintWriter out = fileWriter(prefix + ".csv");
                    P2Stats stats = runP2(pOne, seed, out);
                    finishWriter(out, true);
                    createTimeMemPNGs(stats, prefix);
                    break;
                }
                default:
                    printUsage();
            }
        } catch (IOException ioe) {
            System.err.println("IO error: " + ioe.getMessage());
        }
    }

    private static PrintWriter stdOutWriter() {
        return new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
    }

    private static PrintWriter fileWriter(String path) throws IOException {
        return new PrintWriter(new BufferedWriter(new FileWriter(path)));
    }

    private static void finishWriter(PrintWriter out, boolean close) {
        out.flush();
        if (close) out.close();
    }

    private static void printUsage() {
        PrintWriter u = stdOutWriter();
        u.println("Usage:");
        u.println("  java DPAssign p1 [n] [m] [seed] [out.csv]");
        u.println("  java DPAssign p1batch [seed] [out.csv]");
        u.println("  java DPAssign p2 [p_one] [seed] [out.csv]");
        u.println("  java DPAssign p2png [p_one] [seed] [prefix]");
        u.flush();
    }

    // ------------- Problem 1: weighted approximate common substring -------------
    private static void runP1(int n, int m, long seed, PrintWriter out) {
        Random rng = new Random(seed);
        char[] A = randUpper(n, rng);
        char[] B = randUpper(m, rng);

        out.println("problem,scenario,delta,n,m,best_score,best_len,astart,bstart,asub,bsub,time_ms,mem_bytes");

        int[] w1 = new int[26];
        for (int i = 0; i < 26; i++) w1[i] = 1;
        runP1Once("S1", 10, w1, A, B, out);

        int[] w2 = scaledFreqWeights();
        for (int delta = 1; delta <= 10; delta++) runP1Once("S2", delta, w2, A, B, out);
    }

    private static void runP1Batch(long seed, PrintWriter out) {
        for (int idx = 0; idx < P1_BATCH_SIZES.length; idx++) {
            if (idx > 0) out.println();
            int n = P1_BATCH_SIZES[idx][0];
            int m = P1_BATCH_SIZES[idx][1];
            runP1(n, m, seed, out);
        }
    }

    private static void runP1Once(String scenario, int delta, int[] w, char[] A, char[] B, PrintWriter out) {
        int n = A.length, m = B.length, cols = m + 1;
        int[] dp = new int[(n + 1) * (m + 1)];
        long t0 = System.nanoTime();

        int bestScore = 0, bi = 0, bj = 0;
        for (int i = 1; i <= n; i++) {
            int base = i * cols, prev = (i - 1) * cols;
            char ai = A[i - 1];
            for (int j = 1; j <= m; j++) {
                int s = (ai == B[j - 1]) ? w[ai - 'A'] : -delta;
                int val = dp[prev + (j - 1)] + s;
                if (val < 0) val = 0;
                dp[base + j] = val;
                if (val > bestScore) { bestScore = val; bi = i; bj = j; }
            }
        }

        int i = bi, j = bj, len = 0;
        while (i > 0 && j > 0) {
            int v = dp[i * cols + j];
            if (v <= 0) break;
            len++; i--; j--;
        }
        int aStart, bStart;
        String asub, bsub;
        if (bestScore == 0 || len == 0) {
            aStart = 0; bStart = 0; asub = ""; bsub = "";
        } else {
            aStart = bi - len + 1; bStart = bj - len + 1;
            asub = new String(A, aStart - 1, len);
            bsub = new String(B, bStart - 1, len);
        }

        long t1 = System.nanoTime();
        double ms = (t1 - t0) / 1e6;

        long memBytes = 0L;
        memBytes += 2L * A.length;
        memBytes += 2L * B.length;
        memBytes += 4L * (long)(n + 1) * (m + 1);
        memBytes += 26L * 4L;

        out.println(csv(new String[] {
            "P1", scenario, Integer.toString(delta),
            Integer.toString(n), Integer.toString(m),
            Integer.toString(bestScore), Integer.toString(len),
            Integer.toString(aStart), Integer.toString(bStart),
            trunc(asub), trunc(bsub),
            String.format(Locale.ROOT, "%.3f", ms),
            Long.toString(memBytes)
        }));
    }

    private static char[] randUpper(int n, Random rng) {
        char[] a = new char[n];
        for (int i = 0; i < n; i++) a[i] = (char) ('A' + rng.nextInt(26));
        return a;
    }

    private static int[] scaledFreqWeights() {
        double[] f = {8.167,1.492,2.782,4.253,12.702,2.228,2.015,6.094,6.966,0.153,0.772,4.025,2.406,6.749,7.507,1.929,0.095,5.987,6.327,9.056,2.758,0.978,2.360,0.150,1.974,0.074};
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
        for (double v: f){ if(v<min)min=v; if(v>max)max=v; }
        int[] w = new int[26];
        for (int i=0;i<26;i++){
            double t=(f[i]-min)/(max-min);
            int v=(int)Math.round(1 + 9*t);
            if (v<1) v=1; if (v>10) v=10;
            w[i]=v;
        }
        return w;
    }

    private static String trunc(String s) {
        if (s == null) return "";
        if (s.length() <= 120) return s;
        return s.substring(0,60) + "..." + s.substring(s.length()-60);
    }

    private static String csv(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int k=0;k<fields.length;k++){
            if (k>0) sb.append(',');
            String x = fields[k]==null?"":fields[k];
            boolean q = x.indexOf(',')>=0 || x.indexOf('"')>=0 || x.indexOf('\n')>=0;
            if (!q) sb.append(x);
            else {
                sb.append('"');
                for (int i=0;i<x.length();i++){
                    char c=x.charAt(i);
                    if (c=='"') sb.append("\"\"");
                    else sb.append(c);
                }
                sb.append('"');
            }
        }
        return sb.toString();
    }

    // ------------- Problem 2: largest zero square sub-matrix -------------
    private static P2Stats runP2(double pOne, long seed, PrintWriter out) {
        out.println("problem,m,n,p_one,k,top_i,top_j,time_ms,mem_bytes");
        Random rng = new Random(seed);
        P2Stats stats = new P2Stats(P2_SIZES.length);
        for (int idx = 0; idx < P2_SIZES.length; idx++) {
            int m = P2_SIZES[idx][0];
            int n = P2_SIZES[idx][1];
            byte[] M = new byte[m * n];
            int total = m * n;
            for (int i = 0; i < total; i++) M[i] = (byte)(rng.nextDouble() < pOne ? 1 : 0);
            runP2Once(M, m, n, pOne, out, stats, idx);
        }
        return stats;
    }

    private static void runP2Once(byte[] M, int m, int n, double pOne, PrintWriter out, P2Stats stats, int idx) {
        long t0 = System.nanoTime();
        short[] prev = new short[n + 1], cur = new short[n + 1];
        int bestK = 0, bi = 0, bj = 0;

        for (int i = 1; i <= m; i++) {
            cur[0] = 0;
            int row = (i - 1) * n;
            for (int j = 1; j <= n; j++) {
                if (M[row + (j - 1)] == 1) cur[j] = 0;
                else {
                    short a = prev[j], b = cur[j - 1], c = prev[j - 1];
                    short min = (a < b ? (a < c ? a : c) : (b < c ? b : c));
                    short v = (short)(min + 1);
                    cur[j] = v;
                    if (v > bestK) { bestK = v; bi = i; bj = j; }
                }
            }
            short[] tmp = prev; prev = cur; cur = tmp;
        }

        int topI = (bestK==0)?0:(bi - bestK + 1);
        int topJ = (bestK==0)?0:(bj - bestK + 1);
        long t1 = System.nanoTime();
        double ms = (t1 - t0) / 1e6;
        long memBytes = 1L * m * n + 2L * (n + 1) * 2L;

        stats.mn[idx] = 1L * m * n;
        stats.timeMs[idx] = ms;
        stats.memBytes[idx] = memBytes;

        out.println(csv(new String[] {
            "P2", Integer.toString(m), Integer.toString(n),
            String.format(Locale.ROOT, "%.3f", pOne),
            Integer.toString(bestK), Integer.toString(topI), Integer.toString(topJ),
            String.format(Locale.ROOT, "%.3f", ms),
            Long.toString(memBytes)
        }));
    }

    private static void createTimeMemPNGs(P2Stats stats, String prefix) throws IOException {
        double[] memAsDouble = new double[stats.memBytes.length];
        for (int i = 0; i < memAsDouble.length; i++) memAsDouble[i] = (double) stats.memBytes[i];
        plotSeries(prefix + "_time.png", stats.mn, stats.timeMs, "Problem 2 time vs m*n", "time_ms");
        plotSeries(prefix + "_mem.png", stats.mn, memAsDouble, "Problem 2 memory vs m*n", "mem_bytes");
    }

    private static void plotSeries(String filename, long[] xVals, double[] yVals, String title, String seriesLabel) throws IOException {
        int len = xVals.length;
        if (len == 0) return;
        long minX = xVals[0], maxX = xVals[0];
        double maxY = yVals[0];
        for (int i = 1; i < len; i++) {
            if (xVals[i] < minX) minX = xVals[i];
            if (xVals[i] > maxX) maxX = xVals[i];
            if (yVals[i] > maxY) maxY = yVals[i];
        }
        double yMax = maxY * 1.1;
        if (yMax <= 0.0) yMax = 1.0;
        double xRange = (maxX == minX) ? 1.0 : (double)(maxX - minX);

        int width = 900;
        int height = 600;
        int left = 90;
        int right = width - 60;
        int top = 70;
        int bottom = height - 80;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.setFont(TITLE_FONT);
        g.setColor(Color.BLACK);
        g.drawString(title, left, top - 30);
        g.setFont(LABEL_FONT);
        g.drawString("m*n", (left + right) / 2 - 20, bottom + 45);
        g.drawString(seriesLabel, 10, (top + bottom) / 2);

        g.drawLine(left, top, left, bottom);
        g.drawLine(left, bottom, right, bottom);

        double xStep = (maxX == minX) ? 0.0 : (double)(maxX - minX) / 4.0;
        for (int t = 0; t < 5; t++) {
            double xVal = (maxX == minX) ? minX : (minX + t * xStep);
            if (t == 4) xVal = maxX;
            double frac = (xVal - minX) / xRange;
            int xPix = (int)Math.round(left + frac * (right - left));
            g.drawLine(xPix, bottom, xPix, bottom + 6);
            String label = Long.toString(Math.round(xVal));
            g.drawString(label, xPix - label.length() * 4, bottom + 25);
        }

        double yStep = yMax / 4.0;
        for (int t = 0; t < 5; t++) {
            double yVal = t * yStep;
            if (t == 4) yVal = yMax;
            double frac = yVal / yMax;
            int yPix = (int)Math.round(bottom - frac * (bottom - top));
            g.drawLine(left - 6, yPix, left, yPix);
            String label = String.format(Locale.ROOT, "%.1f", yVal);
            g.drawString(label, left - 10 - label.length() * 7, yPix + 5);
        }

        g.setColor(new Color(0, 102, 204));
        g.setStroke(new BasicStroke(2f));
        int prevX = -1, prevY = -1;
        for (int i = 0; i < len; i++) {
            double fracX = (xVals[i] - minX) / xRange;
            int xPix = (int)Math.round(left + fracX * (right - left));
            double fracY = yVals[i] / yMax;
            int yPix = (int)Math.round(bottom - fracY * (bottom - top));
            if (i > 0) g.drawLine(prevX, prevY, xPix, yPix);
            g.fillOval(xPix - 4, yPix - 4, 8, 8);
            prevX = xPix; prevY = yPix;
        }

        int legendW = 200;
        int legendH = 50;
        int legendX = right - legendW;
        int legendY = top;
        g.setColor(new Color(245, 245, 245));
        g.fillRect(legendX, legendY, legendW, legendH);
        g.setColor(Color.DARK_GRAY);
        g.drawRect(legendX, legendY, legendW, legendH);
        int lineY = legendY + legendH / 2;
        g.setColor(new Color(0, 102, 204));
        g.drawLine(legendX + 15, lineY, legendX + 55, lineY);
        g.fillOval(legendX + 35 - 4, lineY - 4, 8, 8);
        g.setColor(Color.BLACK);
        g.drawString(seriesLabel + " vs m*n", legendX + 70, lineY + 5);

        g.dispose();
        ImageIO.write(img, "png", new java.io.File(filename));
    }

    private static final class P2Stats {
        final long[] mn;
        final double[] timeMs;
        final long[] memBytes;

        P2Stats(int len) {
            mn = new long[len];
            timeMs = new double[len];
            memBytes = new long[len];
        }
    }

    // Example commands:
    //   javac DPAssign.java
    //   java DPAssign p1 1000 1500 42 results_p1.csv
    //   java DPAssign p1batch 42 p1_all.csv
    //   java DPAssign p2 0.4 42 results_p2.csv
    //   java DPAssign p2png 0.4 42 p2_plot
}
