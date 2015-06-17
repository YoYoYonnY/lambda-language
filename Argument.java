public class Argument {
	public static final int RAW     = 0;
	public static final int ERROR   = 1;
	public static final int DEBUG   = 2;
	public static final int COMPILE = 4;
	public static final int OUTPUT  = 8;

	public boolean raw       = true;
	public boolean error     = false;
	public boolean debug     = false;
	public boolean interpret = true;
	public boolean compile   = true;
	public String input;
	public String output;

	public boolean complete() {
		boolean completed = true;
		if (this.input == null) {
			completed = false;
		} else if (this.output == null) {
			this.output = this.input;
		}
		return completed;
	}
}
