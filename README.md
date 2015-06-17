# Lambda (Programming Language)
The Lambda Language by Anne Veling

## Syntax

 * Instructions are separated by newlines, tokens by space.
 * If the last character of an operator is a `:`, it is interpreted as a goto-label. The program can jump to this location by simply calling the operator (without the `:`).
 * If an operator ends with a `!` it is interpreted as the address of that operator, not its current value.
 * If a line contains a `//`, all characters after it are considered comment.

## Predefined labels:

| Name        	| Number 	| Type      	| Description                                                                                                                                                                                                 	|
|-------------	|--------	|-----------	|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------	|
| nop         	| 0      	| operation 	| Does nothing, null-operation; simply updates instruction pointer to next.                                                                                                                                   	|
| read        	| 1      	| operation 	| Reads a value from memory; interprets the next expression result as a memory address and returns the value currently in that memory address.                                                                	|
| load        	| 2      	| operation 	| Loads a value to memory; interprets the next expression result as a memory address, and the expression after that as a value and sets the value of the memory address to that value.                        	|
| add         	| 3      	| operation 	| Adds a value to a memory address; interprets the next expression result as a memory address, and the expression after that as a value and adds the latter value to the value already at the memory address. 	|
| print       	| 4      	| operation 	| Prints the value yielded by the next expression to screen.                                                                                                                                                  	|
| putchar     	| 5      	| operation 	| Writes the character yielded by the next expression to the screen.                                                                                                                                          	|
| getchar     	| 6      	| operation 	| Gets the next character and puts it in the adress yielded by the next expression                                                                                                                            	|
| signof      	| 7      	| operation 	| Returns the sign of a value; interprets the next expression result as the value and returns 1 when it is positive, -1 when it is negative, and 0 when it is zero.                                           	|
| instruction 	| 32     	| operation 	| Holds the address of the current instruction pointer.                                                                                                                                                       	|
| data        	| 34     	| operation 	| Register that holds the address of the next expression start that is set on a jump. This allows procedures to read arguments.                                                                               	|
| result    	| 36     	| operation 	| Holds the value of the current result during the evaluation of an expression.                                                                                                                               	|

## Tutorial (by Anne Veling)
Full tutorial at http://web.archive.org/web/20101127063243/http://www.veling.nl/anne/lang/lambda/

Lambda is a small new programming language inspired on the mathematical lambda calculus combined with a simple Neumann architecture. It contains a small number of operations and a single, linear memory space in which both the processing registers (such as current instruction pointers), the data and the program code resides. This allows lambda to produce self-modifying code, and a direct interference with the programming flow.

I created Lambda because of my love of strange and obfuscated programming languages like Befunge. I like languages that allow the programmer to create his own constructs. Email me with questions, interesting programs or whatever at `anne@veling.nl`

You can download a [Lambda compiler and interpreter](http://web.archive.org/web/20101127063243/http://www.veling.nl/anne/lang/lambda/Lambda.zip) (written in Java) here. The [source code](http://web.archive.org/web/20101127063243/http://www.veling.nl/anne/lang/lambda/Lambda.java.txt) of this interpreter is available too. The application first "compiles" the source code to a list of integers and saves it to disk.

Unfortunately, since the only data type in Lambda is an integer, there is no way to build a standard Hello World program. A `print 7` program, which is its numeric equivalent looks like this:

	print 7

How about that?
Note that because all operators and even the instruction pointer are basically numbers, in Lambda you can do interesting stuff:

	print instruction

will yield the current address of the instruction pointer; where the pointer is (257 in this case since the programming code is loaded starting from address 256).
How about

	load instruction! 256

That is a simple endless loop! It loads the value 256 (the start of the memory range where the program code is loaded; which is the opcode for the `load` instruction). This program thus updates the instruction address (note the `!`) to 256.
If you do

	load instruction 256

the result is interesting; it loads the value 256 to the memory address of the current instruction pointer; that is 257 at that time; it replaces the opcode of `instruction` to 256. This program modifies its own source code! Not that it does anything useful it still is interesting. Maybe one of you can make a self-modifying Lambda program that actually does something interesting when it is run the second time?
A basic procedure that prints out the interesting number 999 can be used as follows:

	//start of program
	print999
	
	back:
	exit
	
	//the declaration of the procedure
	print999:
	print 999
	back
	
	//end of program
	exit:

The `print999` instruction is not known by default, nor a numeric value so it is interpreted as a label. The instruction pointer is updated to the address that is one further than the declaration of the label (otherwise the label itself (which is simply the address) will get interpreted. Thus the program jumps to the procedure, which prints out the value 999. Then it jumps back to the label `back`. Note that it needs to be programmed explicitly. This procedure will always return to the (unique) label `back`. By using the instruction and data pointers better, I believe it is possible to program a better return method (anyone?). It then goes to the `exit` label at the end. The program halts if it walks out of the memory space by interpreting only "0" (`nop`) instructions.
A more interesting procedure that takes an argument is the great `add5` program which adds 5 to any variable! This program can be very useful if you need to add 5 to any value. And you need that often, right? I will leave the `add7` procedure as an exercise to the reader.

	load x! 50
	add5 x
	print x
	
	exit
	
	x: 0
	
	
	add5:
	add read data 5
	load instruction! add data! 1
	
	exit:

Here you see the real power of Lambda at work! To declare a variable `x`, you simply add a label with a value; it is no different than a normal label (remember that a label is simply a pointer to a place in memory). You need to add the `0` after it to initialize the variable and leave room for the value to be updated. Also, you need to make sure that the memory space of the variables is never executed, by making the instruction pointer jump over it.
The program loads the value `50` in the memory address where the label x points to. It then jumps to a label `add5`, keeping the `data` register to the `x` value (that is, the value of the memory address of x).
The `add5` procedure adds 5 to the current value of the variable pointed to by the data register. Note there are implicit braces around this expression: `add (read data) 5`. Afterwards, it jumps back to the address one after the `x` (this is a better return than using the `back` seen before).
