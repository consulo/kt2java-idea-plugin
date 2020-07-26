package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

/**
 * @author VISTALL
 * @since 2020-07-26
 */
public class AnonymousClassExpression extends Expression
{
	private final TypeName myTypeName;

	public AnonymousClassExpression(TypeName typeName)
	{
		myTypeName = typeName;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.add("new $T()", myTypeName);

		return builder.build();
	}
}
