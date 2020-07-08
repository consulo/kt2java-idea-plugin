package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class PrefixExpression extends Expression
{
	private String myToken;
	private GeneratedElement myExpression;

	public PrefixExpression(String token, GeneratedElement expression)
	{
		myToken = token;
		myExpression = expression;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("$L$L", needNewLine), myToken, myExpression.generate());
	}
}
