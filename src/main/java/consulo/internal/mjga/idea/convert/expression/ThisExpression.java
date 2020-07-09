package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;

/**
 * @author VISTALL
 * @since 2020-07-09
 */
public class ThisExpression extends Expression
{
	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of("this");
	}
}
