package com.todoshift.cashbook.core;
import java.math.*;import java.text.*;import java.util.*;
public final class WholeMoney{
 public static String digits(String s){StringBuilder b=new StringBuilder();for(char c:s.toCharArray()){int d=Character.digit(c,10);b.append(d>=0?(char)('0'+d):c);}return b.toString();}
 public static boolean input(String s,boolean signed){return digits(s).matches(signed?"-?[0-9]*":"[0-9]*");}
 public static long parse(String s,boolean signed,Long original){String normalized=digits(s.trim());if(original!=null&&normalized.equals(BigDecimal.valueOf(original,2).stripTrailingZeros().toPlainString()))return original;if(!normalized.matches(signed?"-?[0-9]+":"[0-9]+"))throw new IllegalArgumentException("Whole units required");return Math.multiplyExact(Long.parseLong(normalized),100);}
 public static String edit(long minor){return BigDecimal.valueOf(minor,2).stripTrailingZeros().toPlainString();}
 public static String format(long minor){DecimalFormat f=new DecimalFormat("#,##0.##",DecimalFormatSymbols.getInstance(Locale.ENGLISH));f.setRoundingMode(RoundingMode.UNNECESSARY);return f.format(BigDecimal.valueOf(minor,2));}
}
