package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class BinaryExpression extends Expression
{
	private final GeneratedElement myLeft;
	private final GeneratedElement myRight;
	private final String myToken;

	public BinaryExpression(GeneratedElement left, GeneratedElement right, String token)
	{
		myLeft = left;
		myRight = right;
		myToken = token;
	}

	@Override
	public CodeBlock generate()
	{
		return CodeBlock.of("$L $L $L", myLeft.generate(), myToken, myRight.generate());
	}
}
