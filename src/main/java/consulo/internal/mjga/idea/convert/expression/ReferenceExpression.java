package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.Converter;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class ReferenceExpression extends Expression
{
	private final String myReferenceName;

	public ReferenceExpression(String referenceName)
	{
		myReferenceName = referenceName;
	}

	@Override
	public CodeBlock generate()
	{
		return CodeBlock.of("$L", Converter.safeName(myReferenceName));
	}
}
