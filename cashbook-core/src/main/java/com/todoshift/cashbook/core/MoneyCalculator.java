package com.todoshift.cashbook.core;
import java.math.*;
/** Bounded calculator. Round final result to cents, HALF_UP; never execute input as code. */
public final class MoneyCalculator {
    private final String s;private int pos,depth;
    private MoneyCalculator(String input){
        if(input==null||input.length()>256)throw new IllegalArgumentException("Expression too long");
        StringBuilder b=new StringBuilder();for(char c:input.toCharArray()){
            if(c>='٠'&&c<='٩')c=(char)('0'+c-'٠');if(c>='۰'&&c<='۹')c=(char)('0'+c-'۰');
            if(c=='٫')c='.';if(c=='×')c='*';if(c=='÷')c='/';b.append(c);
        }s=b.toString();
    }
    public static long calculate(String input){MoneyCalculator p=new MoneyCalculator(input);BigDecimal n=p.expression();p.space();if(p.pos!=p.s.length())throw new IllegalArgumentException("Invalid expression");return n.setScale(2,RoundingMode.HALF_UP).movePointRight(2).longValueExact();}
    private void space(){while(pos<s.length()&&Character.isWhitespace(s.charAt(pos)))pos++;}
    private boolean eat(char c){space();if(pos<s.length()&&s.charAt(pos)==c){pos++;return true;}return false;}
    private BigDecimal expression(){BigDecimal n=term();while(true){if(eat('+'))n=n.add(term(),MathContext.DECIMAL128);else if(eat('-'))n=n.subtract(term(),MathContext.DECIMAL128);else return n;}}
    private BigDecimal term(){BigDecimal n=factor();while(true){if(eat('*'))n=n.multiply(factor(),MathContext.DECIMAL128);else if(eat('/'))n=n.divide(factor(),MathContext.DECIMAL128);else return n;}}
    private BigDecimal factor(){if(++depth>32)throw new IllegalArgumentException("Too deep");try{
        if(eat('+'))return factor();if(eat('-'))return factor().negate();
        if(eat('(')){BigDecimal n=expression();if(!eat(')'))throw new IllegalArgumentException("Missing parenthesis");return n;}
        space();int start=pos;while(pos<s.length()&&((s.charAt(pos)>='0'&&s.charAt(pos)<='9')||s.charAt(pos)=='.'))pos++;
        if(start==pos)throw new IllegalArgumentException("Expected number");String token=s.substring(start,pos);
        if(!token.matches("[0-9]+(\\.[0-9]+)?"))throw new IllegalArgumentException("Invalid number");return new BigDecimal(token);
    }finally{depth--;}}
}
