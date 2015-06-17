import com.veling.io.*;
import java.io.*;
import java.util.*;

public class Lambda  {
	//statics
	public static final int NOP = 0;
	public static final int READ = 1;
	public static final int LOAD = 2;
	public static final int ADD = 3;
	public static final int PRINT = 4;
	
	public static final int PUTCHAR = 5;
	public static final int GETCHAR = 6;
	public static final int SIGNOF = 7;

	public static final int INSTRUCTION = 32;
	public static final int DATA = 34;
	public static final int RESULT = 36;
	
	public static final int VALUE = 255;

	public static void main (String[] args)  {
		Argument argument = new Argument();
		ArrayList<Argument> arguments = new ArrayList<Argument>(1);
		if (args.length == 0)  {
			printUsage();
		} else {
			for (int i=0; i < args.length; i++) {
				String arg = args[i];
				int argLength = arg.length();
				if (arg.charAt(0) == '-' && argLength >= 2) {
					if (arg.equals(("-raw-mode").substring(0, argLength))) {
						argument.raw = true;
					} else if (arg.equals(("-error-messages").substring(0, argLength))) {
						argument.error = true;
					} else if (arg.equals(("-debug-messages").substring(0, argLength))) {
						argument.debug = true;
					} else if (arg.equals(("-compile-only").substring(0, argLength))) {
						argument.interpret = false;
					} else if (arg.equals(("-output").substring(0, argLength))) {
						argument.output = args[++i];
					}
				} else {
					argument.input = arg;
					if (argument.complete()) {
						arguments.add(argument);
					}
				}
			}
		}
		for (int i=0; i < arguments.size(); i++) {
			argument = arguments.get(i);
			String compiled = argument.output + ".L";
			compile(argument, compiled);
			load(argument, compiled);
			go(argument);
		}
	}
	
	public static void printUsage() {
		System.out.println("Usages: \n"
			+ "    lambda [file...]\n"
			+ "    lambda -code [code]\n"
			+ "    lambda -r\n"
			+ "  Lambda supports the following flags:\n"
			+ "    -r           Raw mode: Read raw code from stdin\n"
			+ "    -e           Enable error messages\n"
			+ "    -d           Enable debug mode\n"
			+ "    -c           Compile only: Dont interpret\n"
			+ "    -o [file]    Change output file for current file"
		);
	}
	
	public static void compile(Argument argument, String compiledfn)  {
		try {
			FastInputStream in = new FastInputStream(new FileInputStream(argument.input));
			
			HashMap<String, Integer> labels = new HashMap<String, Integer>(100);
			
			labels.put("nop",   new Integer(NOP));
			labels.put("read",  new Integer(READ));
			labels.put("load",  new Integer(LOAD));
			labels.put("add",   new Integer(ADD));
			labels.put("print", new Integer(PRINT));
			labels.put("putchar", new Integer(PUTCHAR));
			labels.put("getchar", new Integer(GETCHAR));
			labels.put("signof",   new Integer(SIGNOF));

			labels.put("instruction", new Integer(INSTRUCTION));
			labels.put("data",        new Integer(DATA));
			labels.put("result",      new Integer(RESULT));
			
			//put all tokens in a list
			ArrayList<String> tokens = new ArrayList<String>(1000);
			String line, token; StringTokenizer tokenizer;
			Integer value; int lastlabel = -1;
			try {
				while (!in.eof) {
					line = in.readLine();
					//skip after comment
					int commentidx = line.indexOf("//");
					if (commentidx>=0)  {
						if (commentidx==0) continue;
						line = line.substring(0,commentidx-1);
					}
					if (line.length()==0) continue;
					if (argument.debug) {
						System.out.println("tokenizing "+(256+tokens.size())+" ["+line+"]");
					}
					tokenizer = new StringTokenizer(line," ()",false);
					while (tokenizer.hasMoreTokens()) {
						token = tokenizer.nextToken();
						if (token.endsWith(":"))  {
							//is a label
							token = token.substring(0,token.length()-1);
							lastlabel = tokens.size();
							labels.put(token,new Integer(256+lastlabel)); //put next address
						} else {
							//check for integer literals
							try {
								value = Integer.decode(token);
								//see if is the first after a label (a variable init)
								tokens.add(value.toString());
								tokens.add(null); //VALUE tag in postfix
								//if (lastlabel!=tokens.size())  {
								//}
							} catch (NumberFormatException e) {
								//probably a label anyway
								//check for value
								if (token.endsWith("!"))  {
									token = token.substring(0,token.length()-1);
									tokens.add(token);
									tokens.add(null);
								} else {
									tokens.add(token);
								}
							}
							lastlabel = -1;
						}
					}
				}
			} catch (EOFException e) {}
			in.close();
			
			System.out.println("compiling...");
			
			FastOutputStream out = new FastOutputStream(new FileOutputStream(compiledfn));
			
			Integer address; Object o;
			for (int i=0; i<tokens.size(); i++) {
				o = tokens.get(i);
				if (o==null)  {
					//add value
					out.writeInt(VALUE);
				} else if (o instanceof String)  {
					token = (String) o;
					//first lookup label
					address = (Integer) labels.get(token);
					if (address!=null)  {
						//recognized label address
						out.writeInt(address.intValue());
					} else {
						//invalid
						if (argument.error) {
							System.out.println("unknown label "+token);
						}
						out.close();
						return;
					}
				} else if (o instanceof Integer) {
					value = (Integer) o;
					out.writeInt(value.intValue());
				} else {
					//error
					if (argument.error) {
						System.out.println("Unknown token type found: "+o);
					}
					out.close();
					return;
				}
			}
			out.close();
			
			System.out.println("compiled.");
			
		} catch (IOException e) {
			if (argument.debug) {
				System.out.println("catched "+e+" with message "+e.getMessage());
			}
		}
	}
	
	protected static int[] memory = new int[1<<16];
	protected static int memorysize;
	
	protected static void load(Argument argument, String fn)  {
		//clear memory
		Arrays.fill(memory,0);
		memorysize = 256;
		try {
			FastInputStream in = new FastInputStream(new FileInputStream(fn));
			
			try {
				while (!in.eof) {
					if (memorysize>=memory.length)  {
						int[] newmem = new int[2*memorysize];
						System.arraycopy(memory,0,newmem,0,memory.length);
						memory = newmem;
					}
					memory[memorysize++] = in.readInt();
				}
			} catch (EOFException e) {}
			
			in.close();
			
			//reset pointers
			memory[INSTRUCTION]   = 256;
			memory[INSTRUCTION+1] = VALUE;
			memory[DATA+1] = VALUE;
			memory[RESULT+1] = VALUE;
			
			memory[RESULT+2] = LOAD;
			memory[RESULT+3] = INSTRUCTION;
			memory[RESULT+4] = VALUE;
			memory[RESULT+5] = 0;
			memory[RESULT+6] = VALUE; //exit
			
			if (argument.debug) {
				System.out.println("read "+(memorysize-256)+" tokens");
			}
		} catch (IOException e) {
			if (argument.debug) {
				System.out.println("catched "+e+" with message "+e.getMessage());
			}
		}
	}
	
	protected static void go(Argument argument)  {
		System.out.println("executing...");
		StringBuffer buf = new StringBuffer(30);
		while ((memory[INSTRUCTION]>0) && (memory[INSTRUCTION]<memorysize)) {
			buf.setLength(0);
			buf.append(memory[INSTRUCTION]+":");
			interpret(argument, buf);
			if (argument.debug) {
				System.out.println(buf);
			}
		}
		if (memory[INSTRUCTION]>=memorysize)  {
			System.out.println("INFINITY; ready.");
		} else {
			System.out.println("ready.");
		}
	}
	
	protected static int get(int idx)  {
		if ((idx>=0) && (idx<memorysize))  {
			return memory[idx];
		} else {
			return 0;
		}
	}

	protected static void put(int idx, int value)  {
		if ((idx>=0) && (idx<memorysize))  {
			memory[idx] = value;
		} else {
			throw new RuntimeException("invalid memory address "+idx+"; cannot put "+value);
		}
	}

	
	protected static void interpret(Argument argument, StringBuffer buf)  {
		int opcode = get(memory[INSTRUCTION]);
		
		//lookahead
		if (get(memory[INSTRUCTION]+1)==VALUE)  {
			//so this one is a value after all
			memory[RESULT] = opcode;
			buf.append(" "+memory[RESULT]);
			memory[INSTRUCTION]+=2;
			return;
		}
		//if i get here, then is not a value but operand/address
		int address, value;
		switch (opcode) {
			case NOP:
				memory[INSTRUCTION]++;
				buf.append(" nop");
				break;
			case READ:
				//evaluate next and interpret result as mem address
				//give back content of that address
				memory[INSTRUCTION]++;
				buf.append(" (read");
				interpret(argument, buf);
				buf.append(")");
				memory[RESULT] = get(memory[RESULT]);
				break;
			case LOAD:
				//evaluate two parameters and interpret first one as
				//address, second as value to load in it
				memory[INSTRUCTION]++;
				buf.append(" load (");
				interpret(argument, buf);
				buf.append(") (");
				address = memory[RESULT];
				interpret(argument, buf);
				buf.append(")");
				value = memory[RESULT];
				//System.out.println("load buffer: "+buf);
				put(address,value);
				if (argument.debug) {
					System.out.println("["+address+"]<--"+value);
				}
				break;
			case ADD:
				//evaluate next two, interpret first as address, 2nd as value
				//add 2nd to value at 1st address and give back as result as well
				memory[INSTRUCTION]++;
				buf.append(" add (");
				interpret(argument, buf);
				buf.append(") (");
				address = memory[RESULT];
				interpret(argument, buf);
				buf.append(")");
				value = memory[RESULT];
				put(address,get(address)+value);
				memory[RESULT] = get(address);
				if (argument.debug) {
					System.out.println("["+address+"]+="+value);
				}
				break;
			case PRINT:
				//evaluate what's next and print that value as a integer
				memory[INSTRUCTION]++;
				buf.append(" print");
				interpret(argument, buf);
				if (argument.debug) {
					System.out.println("-->"+memory[RESULT]);
				} else {
					System.out.println(memory[RESULT]);
				}
				break;
			case PUTCHAR:
				//evaluate what's next and print that value as a character
				memory[INSTRUCTION]++;
				buf.append(" putchar");
				interpret(argument, buf);
				value = memory[RESULT];
				if (argument.debug) {
					System.out.println("-->'"+Character.toString((char)value)+"'");
				} else {
					System.out.print(Character.toString((char)value));
					break;
				}
				break;
			case GETCHAR:
				//get a character from input and put it in the adress given with argument one
				memory[INSTRUCTION]++;
				buf.append(" getchar");
				interpret(argument, buf);
				address = memory[RESULT];
				try {
					do {
						value = System.in.read();
					} while (value == 13);
					put(address, value);
					//System.out.println("getchar buffer: "+buf);
					//evaluate what's next and print that value
					if (argument.debug) {
						System.out.println("["+address+"]<--" + value);
					}
				} catch (IOException e) {
					System.out.println("catched "+e+" with message "+e.getMessage());
				}
				break;
			case SIGNOF:
				//evalute next and give 1 as result if its not zero
				memory[INSTRUCTION]++;
				buf.append(" signof");
				interpret(argument, buf);
				value = memory[RESULT];
				memory[RESULT] = (int)Math.signum(value);
				if (argument.debug) {
					System.out.println("["+RESULT+"]="+"signof("+value+")");
				}
				break;
			default:
				if (get(opcode+1)==VALUE)  {
					//reference to variable value; do directly
					buf.append(" read "+opcode);
					memory[RESULT] = get(opcode);
					memory[INSTRUCTION]++;
				} else {
					buf.append(" jump "+opcode);
					//this is a goto
					memory[DATA] = memory[INSTRUCTION] + 1;
					//memory[RESULT] = get(opcode);
					memory[INSTRUCTION] = opcode;
					buf.append(" (");
					interpret(argument, buf);
					buf.append(")");
				}
				break;
		}
	}
}
