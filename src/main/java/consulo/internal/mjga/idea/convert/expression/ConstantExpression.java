package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;

/**
 * @author VISTALL
 * @since 2020-07-07
 */
public class ConstantExpression extends Expression
{
	public static final ConstantExpression NULL = new ConstantExpression("null");

	public static boolean isNull(GeneratedElement e)
	{
		return e instanceof ConstantExpression && ((ConstantExpression) e).isNull();
	}

	private final String myText;

	public ConstantExpression(String text)
	{
		myText = text;
	}

	public boolean isNull()
	{
		return myText.equals("null");
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("$L", needNewLine), myText);
	}
}
