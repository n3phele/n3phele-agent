/**
 * @author Nigel Cook
 *
 * (C) Copyright 2010-2011. All rights reserved.
 */
package n3phele.agent.repohandlers;

public class Helper {
	public static String wildcardToRegex(String wildcard){
        StringBuffer s = new StringBuffer(wildcard.length());
        boolean alternation = false;
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch(c) {
                case '*':
                    s.append(".*");
                    break;
                case '?':
                    s.append(".");
                    break;
                    // escape special regexp-characters
                case '(': case ')': case '$': case '[': case ']':
                case '^': case '.': case '|':
                case '\\':
                    s.append("\\");
                    s.append(c);
                    break;
                case '{':
                	if(alternation) s.append("\\{");
                	else s.append("(");
                	alternation=true;
                	break;
                case ',':
                	if(alternation) s.append("|");
                	else s.append(",");
                	break;
                case '}':
                	if(alternation) s.append(")");
                	else s.append("\\}");
                	alternation=false;
                	break;
                default:
                    s.append(c);
                    break;
            }
        }
        s.append('$');
        return(s.toString());
    }

}
