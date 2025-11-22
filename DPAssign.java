import java.util.*;

public class DPAssign {
    public static void main(String[] args) {
        if (args.length == 0) {
            runP1(2000, 2500, 123L);
            runP2(0.4, 123L);
            return;
        }
        switch (args[0]) {
            case "p1": {
                int n = (args.length > 1) ? Integer.parseInt(args[1]) : 2000;
                int m = (args.length > 2) ? Integer.parseInt(args[2]) : 2500;
                long seed = (args.length > 3) ? Long.parseLong(args[3]) : 123L;
                runP1(n, m, seed);
                break;
            }
            case "p2": {
                double pOne = (args.length > 1) ? Double.parseDouble(args[1]) : 0.4;
                long seed = (args.length > 2) ? Long.parseLong(args[2]) : 123L;
                runP2(pOne, seed);
                break;
            }
            default:
                System.out.println("Usage:\n  java DPAssign p1 [n] [m] [seed]\n  java DPAssign p2 [p_one] [seed]");
        }
    }

    // ------------- Problem 1: weighted approximate common substring -------------
    // DP: dp[i][j] = max(0, dp[i-1][j-1] + s(i,j)), s = w[char] if match else -delta.
    private static void runP1(int n, int m, long seed) {
        Random rng = new Random(seed);
        char[] A = randUpper(n, rng);
        char[] B = randUpper(m, rng);

        System.out.println("problem,scenario,delta,n,m,best_score,best_len,astart,bstart,asub,bsub,time_ms,mem_bytes");

        // Scenario 1: w=1, delta=10
        int[] w1 = new int[26];
        for (int i = 0; i < 26; i++) w1[i] = 1;
        runP1Once("S1", 10, w1, A, B);

        // Scenario 2: weights from English frequencies scaled to [1,10]; deltas 1..10
        int[] w2 = scaledFreqWeights();
        for (int delta = 1; delta <= 10; delta++) runP1Once("S2", delta, w2, A, B);
    }

    private static void runP1Once(String scenario, int delta, int[] w, char[] A, char[] B) {
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

        // Diagonal backtrack until dp==0 to get length
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
            aStart = bi - len + 1; bStart = bj - len + 1; // 1-based
            asub = new String(A, aStart - 1, len);
            bsub = new String(B, bStart - 1, len);
        }

        long t1 = System.nanoTime();
        double ms = (t1 - t0) / 1e6;

        long memBytes = 0L;
        memBytes += 2L * A.length;                  // chars ~2 bytes each
        memBytes += 2L * B.length;
        memBytes += 4L * (long)(n + 1) * (m + 1);   // int dp
        memBytes += 26L * 4L;                       // weights

        System.out.println(csv(new String[] {
            "P1", scenario, Integer.toString(delta),
            Integer.toString(n), Integer.toString(m),
            Integer.toString(bestScore), Integer.toString(len),
            Integer.toString(aStart), Integer.toString(bStart),
            trunc(asub), trunc(bsub),
            String.format(java.util.Locale.ROOT, "%.3f", ms),
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
    private static void runP2(double pOne, long seed) {
        System.out.println("problem,m,n,p_one,k,top_i,top_j,time_ms,mem_bytes");
        int[][] sizes = {{10,10},{10,100},{10,1000},{100,1000},{1000,1000}};
        Random rng = new Random(seed);
        for (int[] sz : sizes) {
            int m = sz[0], n = sz[1];
            byte[] M = new byte[m * n];
            for (int i = 0; i < m * n; i++) M[i] = (byte)(rng.nextDouble() < pOne ? 1 : 0);
            runP2Once(M, m, n, pOne);
        }
    }

    private static void runP2Once(byte[] M, int m, int n, double pOne) {
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
        long memBytes = 1L * m * n + 2L * (n + 1) * 2L; // matrix bytes + two short rows

        System.out.println(csv(new String[] {
            "P2", Integer.toString(m), Integer.toString(n),
            String.format(java.util.Locale.ROOT, "%.3f", pOne),
            Integer.toString(bestK), Integer.toString(topI), Integer.toString(topJ),
            String.format(java.util.Locale.ROOT, "%.3f", ms),
            Long.toString(memBytes)
        }));
    }
}
