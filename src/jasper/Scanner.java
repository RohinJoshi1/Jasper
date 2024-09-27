package jasper;
import java.util.*;

import static jasper.TokenType.*;

public class Scanner {
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("func",    FUNC);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
    }

    private final String source;
    private final List<Token> tokenList;
    private int start =0;
    private int current = 0;
    private  int line =1;

    public Scanner(String source) {
        this.source = source;
        this.tokenList = new ArrayList<>();
    }
    private boolean isAtEnd(){
        return  current >= source.length();
    }
    List<Token> scanTokens(){
        while(!isAtEnd()){
            start = current;
            scanToken();
        }
        tokenList.add(new Token(EOF, "",null, line));
        return  tokenList;
    }
    private char advance(){
        return source.charAt(current++);
    }
    private void scanToken(){
        char c = advance();
        switch (c){
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            //Coupled sequences
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '#':
                while(peek(0)!='\n' && !isAtEnd())advance();
                break;
            case '/':
                if (match('/')) {
                    while (peek(0) != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;
            case '|':
                //Maximal munch , so this would work for both | and ||
                addToken(OR);
                break;
            case '&':
                addToken(AND);
                break;
            case '\\':

            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;
            case '"':
                //Paragraph comments python inspired
                if(peek(1)=='"' && peek(2)=='"'){
                    do advance();
                    while (peek(0)=='"' && peek(1) =='"' && peek(2)=='"');
                }
                else{
                    string();
                }
                break;

            default:
                if(isDigit(c)){
                    number();
                }else if (isAlpha(c)) {
                    identifier();
                }
                else Jasper.error(line, "Unexpected token");break;
        }
    }
    private void identifier() {
        while (isAlphaNumeric(peek(0))) advance();
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }
    private boolean isDigit(char c){
        return (c <= '9' && c >='0');
    }
    private void number(){
        while(isDigit(peek(0))) advance();
        if(peek(0) == '.' && isDigit(peek(1))){
            do advance();
            while (isDigit(peek(0)));
        }
        addToken(NUMBER, Double.parseDouble(source.substring(start,current)));
    }
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void string(){
        while(peek(0) != '"' && !isAtEnd()){
            if(peek(0) == '\n')line++;
            advance();
        }
        boolean error = false;
        if(isAtEnd()){
            Jasper.error(line, "Unterminated String");
            error = true;
        }
        if(!error){
            advance();
            String val = source.substring(start+1,current-1);
            addToken(STRING, val);
        }

    }
    private char peek(int offset){
        if(current + offset >= source.length())return '\0';
        return  source.charAt(current+offset);
    }
    private boolean match(char c){
        if(isAtEnd())return false;
        if(source.charAt(current) != c) return  false;
        current++;
        return true;
    }
    private void addToken(TokenType type){
        addToken(type, null);
    }
    private void addToken(TokenType type,Object literal){
        String s = source.substring(start,current);
        tokenList.add(new Token(type, s, literal, line));
    }
}
