import java.util.*;

/**
 * Starter code for CS241 assignments 9-11 for Spring 2011.
 * 
 * Based on Scheme code by Gord Cormack. Java translation by Ondrej Lhotak.
 * 
 * Version 20081105.1
 *
 * Modified June 30, 2011 by Brad Lushman
 */
 
 //github harrywzh: Own written code are in between the OWN CODE tags noted below and comprise the majority of the code
 //Starter code as noted above is anything outside of those tags
 
 
public class WLPPGen {
    Scanner in = new Scanner(System.in);

    // The set of terminal symbols in the WLPP grammar.
    Set<String> terminals = new HashSet<String>(Arrays.asList("BOF", "BECOMES", 
         "COMMA", "ELSE", "EOF", "EQ", "GE", "GT", "ID", "IF", "INT", "LBRACE", 
         "LE", "LPAREN", "LT", "MINUS", "NE", "NUM", "PCT", "PLUS", "PRINTLN",
         "RBRACE", "RETURN", "RPAREN", "SEMI", "SLASH", "STAR", "WAIN", "WHILE",
         "AMP", "LBRACK", "RBRACK", "NEW", "DELETE", "NULL"));

    List<String> symbols;

    // Data structure for storing the parse tree.
    public class Tree {
        List<String> rule;

        ArrayList<Tree> children = new ArrayList<Tree>();

        // Does this node's rule match otherRule?
        boolean matches(String otherRule) {
            return tokenize(otherRule).equals(rule);
        }
		String getFirst(){
			return rule.get(0);
		}
		boolean checkFirst(String s){
			return s.equals(rule.get(0));
		}
    }
	
	public class Var {
        String type;
		int addr;
    }

	private int addr = 0;
    // Divide a string into a list of tokens.
    List<String> tokenize(String line) {
        List<String> ret = new ArrayList<String>();
        Scanner sc = new Scanner(line);
        while (sc.hasNext()) {
            ret.add(sc.next());
        }
        return ret;
    }

    // Read and return wlppi parse tree
    Tree readParse(String lhs) {
        String line = in.nextLine();
        List<String> tokens = tokenize(line);
        Tree ret = new Tree();
        ret.rule = tokens;
        if (!terminals.contains(lhs)) {
            Scanner sc = new Scanner(line);
            sc.next(); // discard lhs
            while (sc.hasNext()) {
                String s = sc.next();
                ret.children.add(readParse(s));
            }
        }
        return ret;
    }

    // Compute symbols defined in t
    List<String> genSymbols(Tree t) {
        return null;
    }
	
//--BEGIN OWN CODE 1--
	void findSymbols(Tree t, String reqType){
		if (t.checkFirst("dcl")){
			String type = (t.children.get(0).rule.size() == 2) ? "int" : "int*";
			if (!type.equals(reqType)) bail("Incorrrect declaration type");
		}
		findSymbols(t);
	}
	void findSymbols(Tree t){
		if (t.checkFirst("dcl")){
			String type = (t.children.get(0).rule.size() == 2) ? "int" : "int*";
			String id = t.children.get(1).rule.get(1);
			if (symbolMap.containsKey(id)){
				bail("Already declared " + id);
			} else {
				Var v = new Var();
				v.type = type;
				v.addr = addr;
				addr += 4;
				//System.out.println(id + " " + v.type + " " + v.addr);
				symbolMap.put(id, v);
			}
		} else if (t.checkFirst("expr") || t.checkFirst("lvalue")){
			checkType(t);
		} else if (t.checkFirst("test")){
			if (!checkType(t.children.get(0)).equals(checkType(t.children.get(2)))) bail("Not matching - test");
		} else if (t.checkFirst("statement")){
			if (t.matches("statement lvalue BECOMES expr SEMI")){
				if (!checkType(t.children.get(0)).equals(checkType(t.children.get(2)))) bail("Not matching - statement");
			} else if (t.matches("statement PRINTLN LPAREN expr RPAREN SEMI")){
				if (!checkType(t.children.get(2)).equals("int")) bail("Not matching - statement"); 
			} else if (t.matches("statement PRINTLN LPAREN expr RPAREN SEMI")){
				if (!checkType(t.children.get(3)).equals("int*")) bail("Not matching - statement");
			} else {
				for (Tree c : t.children){
					findSymbols(c);
				}
			}
		} else if (t.checkFirst("dcls")){
			if (t.matches("dcls dcls dcl BECOMES NUM SEMI")){
				for (Tree c : t.children){
					findSymbols(c,"int");
				}
			} else if (t.matches("dcls dcls dcl BECOMES NULL SEMI")){
				for (Tree c : t.children){
					findSymbols(c,"int*");
				}
			}
		} else if (t.checkFirst("procedure")){
			int counter = 0;
			for (Tree c : t.children){
				if (counter == 5){
					findSymbols(c,"int");
				} else if (counter == 11){
					if (!checkType(c).equals("int")) bail("Return value not int");
				} else {
					findSymbols(c);
				}
				counter++;
			}
		
		} else {
			for (Tree c : t.children){
				findSymbols(c);
			}
		}
	}
	
	String checkType(Tree t){
		if (t.checkFirst("ID")){
			String val = t.rule.get(1);
			if (symbolMap.containsKey(val)) {
				return symbolMap.get(val).type;
			} else {
				bail("Undeclared id " + val);
			}
		} else if (t.checkFirst("NUM")){
			return "int";
		} else if (t.checkFirst("NULL")){
			return "int*";
		} else if (t.checkFirst("factor")){
			if (t.matches("factor AMP lvalue")){
				return ifInt(checkType(t.children.get(1)));
			} else if (t.matches("factor STAR factor")){
				return ifIntStar(checkType(t.children.get(1)));
			} else if (t.matches("factor LPAREN expr RPAREN")){
				return checkType(t.children.get(1));
			} else if (t.matches("factor NEW INT LBRACK expr RBRACK")){
				return ifInt(checkType(t.children.get(3)));
			} else {
				return checkType(t.children.get(0));
			}
		} else if (t.checkFirst("lvalue")){
			if (t.matches("lvalue LPAREN lvalue RPAREN")){
				return checkType(t.children.get(1));
			} else if (t.matches("lvalue STAR factor")){
				return ifIntStar(checkType(t.children.get(1)));
			} else {
				return checkType(t.children.get(0));
			}
		} else if (t.checkFirst("term")){
			if (t.matches("term factor")){
				return checkType(t.children.get(0));
			} else {
				ifInt(checkType(t.children.get(0)));
				ifInt(checkType(t.children.get(2)));
				return "int";
			}
		} else if (t.checkFirst("expr")){
			if (t.matches("expr term")){
				return checkType(t.children.get(0));
			} else if (t.matches("expr expr PLUS term")){
				String dexpr = checkType(t.children.get(0));
				String dterm = checkType(t.children.get(2));
				if (dexpr.equals("int") && dterm.equals("int")){
					return "int";
				} else if (dexpr.equals("int") || dterm.equals("int")){
					return "int*";
				}
			} else if (t.matches("expr expr MINUS term")){
				String dexpr = checkType(t.children.get(0));
				String dterm = checkType(t.children.get(2));
				if (dexpr.equals("int*") && dterm.equals("int")){
					return "int*";
				} else if (dexpr.equals(dterm)){
					return "int";
				}
			}
			bail("Expr not matched");
		}
		System.err.println(t.rule);
		bail("Rules exhausted");
		return null;
	}
	
	String ifInt(String type){
		if (!type.equals("int")) bail("Found int* when expecting int");
		return "int*";
	}
	String ifIntStar(String type){
		if (!type.equals("int*")) bail("Found int when expecting int*");
		return "int";
	}
	int expectId = 0;
	HashMap<String,Var> symbolMap = new HashMap<String, Var>();
	
//--END OWN CODE 1--
	
    // Print an error message and exit the program.
    void bail(String msg) {
        System.err.println("ERROR: " + msg);
        System.exit(0);
    }
	void printTree(Tree t) {
		System.out.println(t.rule);
		for (Tree c : t.children){
			printTree(c);
		}
	}

	void printSymbols(){
		for (Map.Entry<String,Var> entry : symbolMap.entrySet()){
			System.err.println(entry.getKey() + " " + entry.getValue().type);
		}
	}
//--BEGIN OWN CODE 2--	
	int deref = 0;
	boolean isPtr = false;
    // Generate the code for the parse tree t.
    String genCode(Tree t) {
		if (t.checkFirst("procedure")){
			System.out.println("sw $1,4($29)");
			System.out.println("sw $2,8($29)");
			if (t.children.get(3).children.get(0).children.size() < 2) System.out.println("add $2,$0,$0");
			System.out.println("lis $6\n.word init\njalr $6");
			System.out.println(";dcls");
			genCode(t.children.get(8)); //dcls
			System.out.println(";statements");
			genCode(t.children.get(9)); //statements
			System.out.println(";return");
			genCode(t.children.get(11)); //RETURN expr		
			
		} else if (t.checkFirst("dcls")){
			if (t.rule.size() > 1){
				genCode(t.children.get(0));
				String id = t.children.get(1).children.get(1).rule.get(1);
				int location = symbolMap.get(id).addr;
				System.out.println("lis $6\n.word " + ((t.matches("dcls dcls dcl BECOMES NUM SEMI")) ? t.children.get(3).rule.get(1) : 0));
				System.out.println("lis $8\n.word " + location);
				System.out.println("add $8,$8,$29");
				storevar(8,6);
			}
		} else if (t.checkFirst("statements")){
			if(!t.children.isEmpty()){
				genCode(t.children.get(0));
				genCode(t.children.get(1));
			}
		} else if (t.checkFirst("dcl")){
		} else if (t.checkFirst("lvalue")){
			if (t.children.size() == 1){
				genCode(t.children.get(0));
			} else {
				if (t.children.size() == 2) deref++;
				genCode(t.children.get(1));
				if (deref > 0){
					dereference(3);
					System.out.println("add $13,$28,$0");
				}
			}
		} else if (t.checkFirst("statement")){
			System.out.println(";>statement " + t.rule);
			if(t.matches("statement PRINTLN LPAREN expr RPAREN SEMI")){
				genCode(t.children.get(2));
				System.out.println("add $1, $3, $0");
				System.out.println("lis $6\n.word print\njalr $6");
			} else if(t.matches("statement lvalue BECOMES expr SEMI")){
				genCode(t.children.get(0));
				System.out.println("add $16,$13,$0");
				genCode(t.children.get(2));
				storevar(16,3);
			} else if (t.matches("statement WHILE LPAREN test RPAREN LBRACE statements RBRACE")){
				String a = genlabel();
				String b = genlabel();
				System.out.println(a+":");				
				genCode(t.children.get(2));
				System.out.println("beq $3,$0,"+b);
				genCode(t.children.get(5));
				System.out.println("beq $0,$0," + a);
				System.out.println(b+":");
			} else if (t.matches("statement IF LPAREN test RPAREN LBRACE statements RBRACE ELSE LBRACE statements RBRACE")){
				String a = genlabel();
				String b = genlabel();							
				genCode(t.children.get(2));
				System.out.println("beq $3,$0,"+a);
				genCode(t.children.get(5));
				System.out.println("beq $0,$0,"+b);
				System.out.println(a+":");
				genCode(t.children.get(9));
				System.out.println(b+":");
				
			} else if (t.matches("statement DELETE LBRACK RBRACK expr SEMI")){
				genCode(t.children.get(3));
				System.out.println("add $1, $3, $0");
				System.out.println("lis $6\n.word delete\njalr $6");
			}
		} else if (t.checkFirst("test")){
			genCode(t.children.get(0)); 
			push(3);
			boolean firstPtr = isPtr;
			genCode(t.children.get(2));
			pop(6);
			if (isPtr || firstPtr){
				if (t.matches("test expr LT expr")){				
					System.out.println("sltu $3,$6,$3");
				} else if (t.matches("test expr GT expr")){				
					System.out.println("sltu $3,$3,$6");
				} else if (t.matches("test expr LE expr")){				
					System.out.println("sltu $8,$3,$6");
					System.out.println("sltu $3,$8,$11");
				} else if (t.matches("test expr GE expr")){				
					System.out.println("sltu $8,$6,$3");
					System.out.println("sltu $3,$8,$11");
				} else if (t.matches("test expr EQ expr")){				
					System.out.println("sltu $21,$3,$6");
					System.out.println("sltu $22,$6,$3");
					System.out.println("add $8,$21,$22");
					System.out.println("sltu $3,$8,$11");
				} else if (t.matches("test expr NE expr")){				
					System.out.println("sltu $21,$3,$6");
					System.out.println("sltu $22,$6,$3");
					System.out.println("add $3,$21,$22");
				}
			} else {
				if (t.matches("test expr LT expr")){				
					System.out.println("slt $3,$6,$3");
				} else if (t.matches("test expr GT expr")){				
					System.out.println("slt $3,$3,$6");
				} else if (t.matches("test expr LE expr")){				
					System.out.println("slt $8,$3,$6");
					System.out.println("slt $3,$8,$11");
				} else if (t.matches("test expr GE expr")){				
					System.out.println("slt $8,$6,$3");
					System.out.println("slt $3,$8,$11");
				} else if (t.matches("test expr EQ expr")){				
					System.out.println("slt $21,$3,$6");
					System.out.println("slt $22,$6,$3");
					System.out.println("add $8,$21,$22");
					System.out.println("slt $3,$8,$11");
				} else if (t.matches("test expr NE expr")){				
					System.out.println("slt $21,$3,$6");
					System.out.println("slt $22,$6,$3");
					System.out.println("add $3,$21,$22");
				}
			}
		} else if (t.checkFirst("expr")){
			genCode(t.children.get(0)); 
			if (t.rule.size() > 2){
				boolean firstPtr = isPtr;
				isPtr = false;
				push(3);
				genCode(t.children.get(2));
				pop(6);
				if (firstPtr && !isPtr){
					multby4(3);
					System.out.println(((t.matches("expr expr PLUS term")) ? "add" : "sub") + " $3,$6,$3");
					isPtr = true;
				} else if (isPtr && !firstPtr){
					multby4(6);
					System.out.println(((t.matches("expr expr PLUS term")) ? "add" : "sub") + " $3,$6,$3");
					isPtr = true;
				} else if (firstPtr && isPtr){
					System.out.println("sub $3,$6,$3");
					System.out.println("div $3,$4\nmflo $3");
					isPtr = false;
				} else {
					System.out.println(((t.matches("expr expr PLUS term")) ? "add" : "sub") + " $3,$6,$3");
				}
			}
		} else if (t.checkFirst("term")){
			genCode(t.children.get(0)); 
			if (t.rule.size() > 2){
				push(3);
				genCode(t.children.get(2));
				pop(6);
				System.out.println(((t.matches("term term STAR factor")) ? "mult" : "div") + " $6,$3");
				System.out.println(((t.matches("term term PCT factor")) ? "mfhi" : "mflo") + " $3");
			}
		} else if (t.checkFirst("factor")){
			if (t.matches("factor ID")) genCode(t.children.get(0)); //factor -> ID
			else if (t.matches("factor LPAREN expr RPAREN")) genCode(t.children.get(1)); //factor -> LPAREN expr RPAREN
			else if (t.matches("factor NUM")) System.out.println("lis $3\n.word " + t.children.get(0).rule.get(1));
			else if (t.matches("factor NULL")) System.out.println("lis $3\n.word 0");
			else if (t.matches("factor STAR factor")){
				deref++;
				genCode(t.children.get(1));
				if (deref > 0){
					dereference(3);
					System.out.println("add $13,$28,$0");
				}
			} else if (t.matches("factor AMP lvalue")){
				deref--;
				genCode(t.children.get(1));
				isPtr = true;
			} else if (t.matches("factor NEW INT LBRACK expr RBRACK")){
				genCode(t.children.get(3));
				System.out.println("add $1, $3, $0");
				System.out.println("lis $6\n.word new\njalr $6");
			}
		} else if (t.checkFirst("ID")){
			String id = t.rule.get(1);
			String type = symbolMap.get(id).type;
			int location = symbolMap.get(id).addr;
			//if (type.equals("int*")) deref++;
			isPtr = (type.equals("int*")) ? true : false;
			if (deref < 0){
				System.out.println("lis $6\n.word " + location);
				System.out.println("add $3,$6,$29");
				deref = 0;
			} else {
				readvar(location);
				System.out.println("add $13,$28,$0");
			}
		} else if (t.checkFirst("S")){
			int size = 4+4*symbolMap.size();
			System.out.println(".import print");
			System.out.println(".import init");
			System.out.println(".import new");
			System.out.println(".import delete");
			System.out.println("lis $6");
			System.out.println(".word " + size);
			System.out.println("lis $4");
			System.out.println(".word 4");
			System.out.println("lis $11");
			System.out.println(".word 1");
			System.out.println("sub $29,$30,$6");
			System.out.println("add $30,$29,$0");
			push(31);
			
			genCode(t.children.get(1));
			pop(31);
			System.out.println("jr $31");
		}
        return null;
    }

	public void storevar(int memaddr, int reg){
		System.out.println("sw $" + reg + ",0($" + memaddr + ")");
	}

	public void multby4(int reg){
		System.out.println("mult $4,$" + reg);
		System.out.println("mflo $" + reg);	
	}
	public void readvar(int offset){
		System.out.println("lis $6\n.word " + offset);
		System.out.println("add $28,$6,$29");
		System.out.println("lw $3,0($28)");
	}
	
	public void dereference(int offsetreg){
		isPtr = false;
		System.out.println("add $28,$3,$0");
		System.out.println("lw $3,0($28)");
	}
	public void push(int reg){
		System.out.println("sub $30,$30,$4");
		System.out.println("sw $" + reg + ",0($30)");
	}
	public void pop(int reg){		
		System.out.println("lw $" + reg + ",0($30)");
		System.out.println("add $30,$30,$4");
	}
//--END OWN CODE 2

    // Main program
    public static final void main(String args[]) {
        new WLPPGen().go();
    }
	int labelcount = 0;
	private String genlabel(){
		labelcount++;
		return "lbl" + labelcount;
	}

    public void go() {
		addr = 4;
        Tree parseTree = readParse("S");
		findSymbols(parseTree);
		genCode(parseTree);
		//printSymbols();
    }
}

