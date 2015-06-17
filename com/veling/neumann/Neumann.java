package com.veling.neumann;

import com.veling.io.*;
import java.io.*;
import java.util.*;

public class Neumann  {
	public static final int NOP = 0;
	public static final int INC = 1;
	public static final int DEC = 2;
	public static final int COPY = 3;
	public static final int MOVE = 4;
	public static final int LOAD = 5;
	public static final int SET = 6;
	public static final int IFZERO = 7;
	public static final int PRINT = 8;
	
	public static final int INSTRUCTION = 0;
	public static final int DATA = 1;
	public static final int DEBUG = 10;

	public static void main (String[] args)  {
		if (args.length==1)  {
			compileFile(args[0]);
			interpret(args[0]+".neu");
		} else {
			System.out.println("usage: Neumann myfile");
		}
	}
	
	public static void compileFile(String fn)  {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fn));
			
			FastOutputStream writer = new FastOutputStream(new FileOutputStream(fn+".neu"));
			int filepos = 0;
			for (; filepos<256; filepos++) {
				writer.writeInt(0);
			}
			
			HashMap labels = new HashMap();
			
			labels.put("nop",new Integer(NOP));
			labels.put("inc",new Integer(INC));
			labels.put("dec",new Integer(DEC));
			labels.put("copy",new Integer(COPY));
			labels.put("move",new Integer(MOVE));
			labels.put("load",new Integer(LOAD));
			labels.put("set",new Integer(SET));
			labels.put("ifzero",new Integer(IFZERO));
			labels.put("print",new Integer(PRINT));

			labels.put("instruction",new Integer(INSTRUCTION));
			labels.put("data",new Integer(DATA));
			labels.put("debug",new Integer(DEBUG));

			String line,token,label,opcode; StringTokenizer tokenizer;
			reader.mark(1024*1024);
			while ((line = reader.readLine()) != null) {
				//System.out.println("reading ["+line+"]");
				tokenizer = new StringTokenizer(line," ",false);
				if (tokenizer.hasMoreTokens())  {
					token = tokenizer.nextToken();
					if (token.endsWith(":"))  {
						label = token.substring(0,token.length()-1);
						//put in hashmap
						System.out.println("Label ["+label+"] is "+filepos);
						labels.put(label,new Integer(filepos));
						if (tokenizer.hasMoreTokens()) token = tokenizer.nextToken();
						else continue;
					}
					boolean first = true;
					//read opcode; operands
					do {
						if (!first) token = tokenizer.nextToken();
						first = false;
						filepos++;
					} while (tokenizer.hasMoreTokens()); 
				}
			}
			reader.reset(); filepos = 256;
			while ((line = reader.readLine()) != null) {
				//System.out.println("compiling ["+line+"]");
				tokenizer = new StringTokenizer(line," ",false);
				label = null;
				//read opcode
				if (tokenizer.hasMoreTokens())  {
					token = tokenizer.nextToken();
					if (token.endsWith(":"))  {
						if (tokenizer.hasMoreTokens()) token = tokenizer.nextToken();
						else continue;
					}
					boolean first = true;
					//read opcode; operands
					do {
						if (!first) token = tokenizer.nextToken();
						first = false;
						
						//lookup in label
						Integer labelpos = (Integer) labels.get(token);
						if (labelpos!=null)  {
							//System.out.println(filepos+": recognized label ["+token+"] as "+labelpos.intValue());
							writer.writeInt(labelpos.intValue());
							filepos++;
						} else {
							//uknown
							//either label-to-come or integer literal
							try {
								int ival = Integer.parseInt(token);
								//System.out.println(filepos+": integer literal ["+token+"] is "+ival);
								writer.writeInt(ival);
								filepos++;
							} catch (NumberFormatException e) {
								//not valid integer, should be a label
								reader.close();
								writer.close();
								System.out.println(filepos+": unknown label ["+token+"]");
								throw new RuntimeException(filepos+": unknown label["+token+"]");
							}
						}
					} while (tokenizer.hasMoreTokens());
				}
				
			}
			reader.close();
			writer.close();
			System.out.println("Ready compiling");
		} catch (IOException e) {
			System.out.println("catched "+e+" with message "+e.getMessage());
		}
	}
	
	public static int[] memory;
	public static int memorysize;
	
	public static void interpret(String fn)  {
		try {
			System.out.println("Interpreting "+fn);
			FastInputStream in = new FastInputStream(new FileInputStream(fn));
			
			memory = new int[1000];
			memorysize = 0;
			try {
				while (!in.eof) {
					if (memorysize >= memory.length)  {
						int[] newmem = new int[2*memory.length];
						System.arraycopy(memory,0,newmem,0,memory.length);
						memory = newmem;
					}
					//now there's room
					memory[memorysize++] = in.readInt();
				}
			} catch (EOFException e) {}
			
			System.out.println("Read "+memorysize+" positions");
			
			memory[INSTRUCTION] = 256;
			memory[DATA] = 256;
			
			while (validInstruction()) {
				go();
			}
			
			System.out.println("Ready interpreting");
		} catch (IOException e) {
			System.out.println("catched "+e+" with message "+e.getMessage());
		}
	}
	
	protected static int get(int idx)  {
		if ((idx>=0) && (idx<memorysize))  {
			return memory[idx];
		} else {
			return 0;
		}
	}
	
	protected static void put(int idx, int val)  {
		if ((idx>=0) && (idx<memorysize))  {
			memory[idx] = val;
		} else {
			//invalid put
			throw new RuntimeException("Invalid memory address "+idx+"; cannot put "+val);
		}
	}
	
	protected static boolean validInstruction()  {
		return ((memory[INSTRUCTION]>0) && (memory[INSTRUCTION]<memorysize));
	}
	
	protected static boolean debugging()  {
		return memory[DEBUG]==0;
	}
	
	protected static void go()  {
		//do next instruction
		if (debugging()) System.out.print(memory[INSTRUCTION]+":"+memory[DATA]+"\t");
		int opcode = get(memory[INSTRUCTION]);
		int operand1,operand2;
		switch (opcode) {
			case NOP:
				memory[INSTRUCTION]++;
				if (debugging()) System.out.println("nop");
				break;
			case INC:
				//increase first operand by one
				memory[INSTRUCTION] += 2;
				operand1 = get(memory[INSTRUCTION]-1);
				if (debugging()) System.out.println("inc "+operand1+" value="+(get(operand1)+1));
				put(operand1,get(operand1)+1);
				break;
			case DEC:
				//decrease first operand by one
				memory[INSTRUCTION] += 2;
				operand1 = get(memory[INSTRUCTION]-1);
				if (debugging()) System.out.println("dec "+operand1+" value="+(get(operand1)-1));
				put(operand1,get(operand1)-1);
				break;
			case COPY:
				memory[INSTRUCTION] += 3;
				operand1 = get(memory[INSTRUCTION]-2);
				operand2 = get(memory[INSTRUCTION]-1);
				if (debugging()) System.out.println("copy "+operand1+" <- "+operand2+" value="+get(operand2));
				put(operand1,get(operand2));
				break;
			case MOVE:
				//move value of 2nd operand into 1st
				memory[INSTRUCTION] += 3;
				operand1 = get(memory[INSTRUCTION]-2);
				operand2 = get(memory[INSTRUCTION]-1);
				if (debugging()) System.out.println("move "+operand1+" <- "+operand2+" value="+get(get(operand2)));
				put(operand1,get(get(operand2)));
				break;
			case LOAD:
				memory[INSTRUCTION] += 3;
				operand1 = get(memory[INSTRUCTION]-2);
				operand2 = get(memory[INSTRUCTION]-1);
				if (debugging()) System.out.println("load "+operand1+" <- "+operand2+" value="+get(operand2));
				put(get(operand1),get(operand2));
				break;
			case SET:
				//move 2nd operand into 1st
				memory[INSTRUCTION] += 3;
				operand1 = get(memory[INSTRUCTION]-2);
				operand2 = get(memory[INSTRUCTION]-1);
				if (debugging()) System.out.println("set "+operand1+" <- "+operand2);
				put(operand1,operand2);
				break;
			case IFZERO:
				memory[INSTRUCTION] += 2;
				operand1 = get(memory[INSTRUCTION]-1);
				if (debugging()) System.out.println("ifzero "+operand1+" value="+get(operand1));
				if (get(operand1)!=0)  {
					//false so skip next jump
					memory[INSTRUCTION]++;
				}
				break;
			case PRINT:
				//print value of operand
				memory[INSTRUCTION] += 2;
				operand1 = get(memory[INSTRUCTION]-1);
				if (debugging()) System.out.println("print "+operand1+" value="+get(operand1));
				System.out.println("-->"+get(operand1));
				break;
			default:
				//jump
				//change instruction pointer to jump address
				//set data pointer to next (for operands and return)
				memory[DATA] = memory[INSTRUCTION]+1;
				memory[INSTRUCTION] = opcode;
				if (debugging()) System.out.println("goto "+opcode);
				//and go there
				break;
		}
	}
}