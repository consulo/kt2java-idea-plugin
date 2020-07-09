package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.GeneratedElement;

/**
 * @author VISTALL
 * @since 2020-07-09
 */
public class InstanceOfExpression extends Expression
{
	private final GeneratedElement myExpression;
	private final TypeName myTypeName;

	public InstanceOfExpression(GeneratedElement expression, TypeName typeName)
	{
		myExpression = expression;
		myTypeName = typeName;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("$L instanceof $T", needNewLine), myExpression.generate(), myTypeName);
	}
}
