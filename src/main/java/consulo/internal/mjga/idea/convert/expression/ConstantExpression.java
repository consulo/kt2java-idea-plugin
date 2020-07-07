package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;

/**
 * @author VISTALL
 * @since 2020-07-07
 */
public class ConstantExpression extends Expression
{
	private String myText;

	public ConstantExpression(String text)
	{
		myText = text;
	}

	@Override
	public CodeBlock generate()
	{
		return CodeBlock.of("$L", myText);
	}
}
