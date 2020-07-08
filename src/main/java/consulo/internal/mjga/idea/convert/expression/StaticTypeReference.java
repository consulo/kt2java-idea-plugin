package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class StaticTypeReference extends Expression
{
	private TypeName myTypeName;
	private String myName;

	public StaticTypeReference(TypeName typeName, String name)
	{
		myTypeName = typeName;
		myName = name;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("$T.$L", needNewLine), myTypeName, myName);
	}
}
