package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class QualifiedExpression extends Expression
{
	private final GeneratedElement myLeft;
	private final GeneratedElement myRight;

	public QualifiedExpression(GeneratedElement left, GeneratedElement right)
	{
		myLeft = left;
		myRight = right;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("$L.$L", needNewLine), myLeft.generate(), myRight.generate());
	}
}
