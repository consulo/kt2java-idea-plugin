package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;

/**
 * @author VISTALL
 * @since 2020-07-11
 */
public class ParExpression extends Expression
{
	private final GeneratedElement myInner;

	public ParExpression(GeneratedElement inner)
	{
		myInner = inner;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("($L)", needNewLine), myInner.generate());
	}
}
