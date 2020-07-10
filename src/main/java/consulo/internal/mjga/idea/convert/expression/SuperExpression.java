package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;

/**
 * @author VISTALL
 * @since 2020-07-10
 */
public class SuperExpression extends Expression
{
	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("super", needNewLine));
	}
}
