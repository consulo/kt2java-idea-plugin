package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

/**
 * @author VISTALL
 * @since 2020-07-09
 */
public class TypeReferenceExpression extends Expression
{
	private TypeName myTypeName;

	public TypeReferenceExpression(TypeName typeName)
	{
		myTypeName = typeName;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of("$T", myTypeName);
	}
}
