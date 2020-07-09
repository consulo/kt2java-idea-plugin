package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.GeneratedElement;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class StaticTypeQualifiedExpression extends Expression
{
	private TypeName myTypeName;
	private GeneratedElement myNameReference;

	public StaticTypeQualifiedExpression(TypeName typeName, String name)
	{
		myTypeName = typeName;
		myNameReference = new ReferenceExpression(name);
	}

	public StaticTypeQualifiedExpression(TypeName typeName, GeneratedElement name)
	{
		myTypeName = typeName;
		myNameReference = name;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("$T.$L", needNewLine), myTypeName, myNameReference.generate());
	}
}
