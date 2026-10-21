import java.util.Locale;
public class PowerCheck {
    static final char[] SUPERSCRIPT = {'\u2070','\u00b9','\u00b2','\u00b3','\u2074','\u2075','\u2076','\u2077','\u2078','\u2079'};
    static final int EXPONENT_DECIMALS = 3;
    static String superscript(String s){StringBuilder sb=new StringBuilder();for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c=='.')sb.append('\u00b7');else if(c>='0'&&c<='9')sb.append(SUPERSCRIPT[c-'0']);else sb.append(c);}return sb.toString();}
    static String powerOfTenWord(int n) {
        int digitExponent = Integer.toString(n).length() - 1;
        long pow = 1;
        for (int i = 0; i < digitExponent; i++) pow *= 10;
        if (n == pow) return "10" + superscript(Integer.toString(digitExponent)) + "x";
        double exponent = Math.log10(n);
        String expStr = String.format(Locale.US, "%." + EXPONENT_DECIMALS + "f", exponent);
        return "10" + superscript(expStr) + "x";
    }
    static int fails=0;
    static void e(int n,String want){String g=powerOfTenWord(n);if(!g.equals(want)){System.out.printf("FAIL %d want %s got %s%n",n,want,g);fails++;}}
    public static void main(String[] a){
        e(1000,"10\u00b3x");
        e(10000,"10\u2074x");
        e(100000,"10\u2075x");
        e(1000000,"10\u2076x");
        e(1500,"10\u00b3\u00b7\u00b9\u2077\u2076x");
        e(2000,"10\u00b3\u00b7\u00b3\u2070\u00b9x");
        e(1001,"10\u00b3\u00b7\u2070\u2070\u2070x");
        e(5000,"10\u00b3\u00b7\u2076\u2079\u2079x");
        e(12345,"10\u2074\u00b7\u2070\u2079\u00b9x");
        e(2000000000,"10\u2079\u00b7\u00b3\u2070\u00b9x");
        System.out.println(fails==0?"ALL PASS":(fails+" FAILURES"));
        for(int n: new int[]{1000,1001,1500,2000,5000,9999,10000,12345,100000,1000000,2000000000})
            System.out.println(n+" -> "+esc(powerOfTenWord(n))+" Compressed Cobblestone");
    }
    static String esc(String s){StringBuilder b=new StringBuilder();for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c<128)b.append(c);else b.append(String.format("\\u%04x",(int)c));}return b.toString();}
}
