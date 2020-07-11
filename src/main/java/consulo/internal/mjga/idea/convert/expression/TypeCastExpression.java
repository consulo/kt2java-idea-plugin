package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.GeneratedElement;

/**
 * @author VISTALL
 * @since 2020-07-11
 */
public class TypeCastExpression extends Expression
{
	private final TypeName myType;
	private final GeneratedElement myElement;

	public TypeCastExpression(TypeName type, GeneratedElement element)
	{
		myType = type;
		myElement = element;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("($T) $L", needNewLine), myType, myElement.generate());
	}
}
