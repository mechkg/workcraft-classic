package workcraft.util;
import java.util.Stack;

public class MatrixStack {
	private Stack<Mat4x4> stack;
	
	public MatrixStack() {
		stack = new Stack<Mat4x4>();
		pushIdentity();
	}
	
	public Mat4x4 top() {
		return stack.peek();
	}
	
	public Mat4x4 pop() {
		return stack.pop();
	}
	
	public Mat4x4 push() {
		return stack.push(new Mat4x4(stack.peek()));	
	}
	
	public Mat4x4 pushIdentity() {
		return stack.push(new Mat4x4());
	}
}