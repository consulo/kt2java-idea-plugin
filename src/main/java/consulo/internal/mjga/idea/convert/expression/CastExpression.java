package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.GeneratedElement;

/**
 * @author VISTALL
 * @since 31/12/2020
 */
public class CastExpression extends Expression
{
	private final TypeName myTypeName;
	private final GeneratedElement myExpression;

	public CastExpression(TypeName typeName, GeneratedElement expression)
	{
		myTypeName = typeName;
		myExpression = expression;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("($T) $L", needNewLine), myTypeName, myExpression.generate());
	}
}
