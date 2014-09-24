import java.util.*;
import java.io.*;


public class WLPPParse {
	
	//Debug - Ensure no accidental debug output
	private boolean debug = false;  //Global Debug Config Here
	private void printdebug(String s){		
		if (debug) System.out.println(s);		
	}	

    public static final void main(String[] args) {
        new WLPPParse().run();
    }

	//private HashMap<String,HashMap<String,String>> nodeMap = new HashMap<String,HashMap<String,String>>();
    private ArrayList<String[]> rules = new ArrayList<String[]>();
	private ArrayList<String> ruleStr = new ArrayList<String>();
	
	private void run() {
		try{
        Scanner in = new Scanner(new FileReader("WLPP.lr1"));
		int counter = 0;
		
		//terminals
		String line = in.nextLine();
		int read = Integer.parseInt(line);
		for (int i = 0; i<read; i++){
			line = in.nextLine();
		}
		
		//non terminals
		line = in.nextLine();
		read = Integer.parseInt(line);
		for (int i = 0; i<read; i++){
			line = in.nextLine();
		}
		
		//start symbol
		line = in.nextLine();
		String startSymbol = line;
		String startRule = "";
		
		//rules
		line = in.nextLine();
		read = Integer.parseInt(line);
		for (int i = 0; i<read; i++){
			line = in.nextLine();
			ruleStr.add(line);
			String[] arr = line.split(" ");
			if (arr[0].equals(startSymbol)){
				startRule = line;
			}
			rules.add(arr);
		}
		
		//states
		line = in.nextLine();
		int numOfStates = Integer.parseInt(line); 
		ArrayList<HashMap<String, Integer>> transitions = new ArrayList<HashMap<String, Integer>>();
		ArrayList<HashMap<String, Integer>> reductions = new ArrayList<HashMap<String, Integer>>();
		for (int i = 0; i < numOfStates; i++){
			transitions.add(new HashMap<String,Integer>());
			reductions.add(new HashMap<String,Integer>());
		}
		
		
		//actions
		line = in.nextLine();
		read = Integer.parseInt(line);
		for (int i = 0; i<read; i++){
			line = in.nextLine();
			String[] actionArr = line.split(" ");
			int n1 = Integer.parseInt(actionArr[0]);
			int n2 = Integer.parseInt(actionArr[3]);
			if (actionArr[2].equals("reduce")){
				reductions.get(n1).put(actionArr[1], n2);
			} else {
				transitions.get(n1).put(actionArr[1], n2);
			}
		}
		in.close();
		
		Stack<Node> nodeStack = new Stack<Node>();
		Stack<Integer> stateStack = new Stack<Integer>();
		
		
		Scanner in2 = new Scanner(System.in);
		System.out.println(startRule);
		nodeStack.push(new Node("BOF BOF"));
		stateStack.push(transitions.get(0).get("BOF"));
		//printdebug("BOF BOF");
		counter = 0;
		//String outbuffer = "procedure INT WAIN LPAREN dcl COMMA dcl RPAREN LBRACE dcls statements RETURN expr SEMI RBRACE";
		//line to parse	
		boolean isEOF = false;
		while (in2.hasNext() || isEOF){
			line = isEOF ? "EOF EOF" : in2.nextLine();
			String[] inSym = line.split(" ");			
			for (int i = 0; i < inSym.length; i+=2){
				String a = inSym[i];
				Integer redState = reductions.get(stateStack.peek()).get(a);
				
				
				while (redState != null){
					
					String[] rule = rules.get(redState);
					//Node lhsRule = new Node(rule[0]);
					Node lhsRule = new Node(ruleStr.get(redState));
					for (int j = 0; j < rule.length - 1; j++){
						lhsRule.addChild(nodeStack.pop());
						stateStack.pop();
					}
					nodeStack.push(lhsRule);
					stateStack.push(transitions.get(stateStack.peek()).get(rule[0]));
					//printdebug(ruleStr.get(redState));
					redState = reductions.get(stateStack.peek()).get(a);
				}
					//printdebug(a + " " + inSym[i+1]);
				nodeStack.push(new Node(a + " " + inSym[i+1]));
				Integer newState = transitions.get(stateStack.peek()).get(a);
				if (newState == null){
					System.err.println("ERROR at " + (counter+1));
					System.exit(1);
				} else {
					stateStack.push(newState);
					counter++;
				}
			}
			isEOF = isEOF ? false : !in2.hasNext();
		}
		
		for (Node n : nodeStack){
			//printdebug(">>Stack node");
			printNode(n);
		}
		
		} catch (Exception e){
			e.printStackTrace();
		}
    }
	
	private void printNode(Node n){
		System.out.println(n.name);
		for (Node c : n.children){
			printNode(c);
		}	
	}
	

}
class Node {	
	public Node(String s){
		children = new LinkedList<Node>();
		name = s;
	}
	public void addChild(Node n){
		children.addFirst(n);
	}
	public String name;
	public LinkedList<Node> children;

}