package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * @author VISTALL
 * @since 2020-07-26
 */
public class AnonymousClassExpression extends Expression
{
	private final TypeName myTypeName;
	private final TypeSpec.Builder myAnonymClassSpec;

	public AnonymousClassExpression(TypeName typeName, TypeSpec.Builder anonymClassSpec)
	{
		myTypeName = typeName;
		myAnonymClassSpec = anonymClassSpec;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		CodeBlock.Builder builder = CodeBlock.builder();
		if (myAnonymClassSpec != null)
		{
			builder.add("$L", myAnonymClassSpec.build());
		}
		else
		{
			builder.add("new $T()", myTypeName);
		}

		return builder.build();
	}
}
