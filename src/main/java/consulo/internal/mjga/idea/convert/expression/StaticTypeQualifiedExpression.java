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
	private GeneratedElement mySelector;

	public StaticTypeQualifiedExpression(TypeName typeName, String name)
	{
		myTypeName = typeName;
		mySelector = new ReferenceExpression(name);
	}

	public StaticTypeQualifiedExpression(TypeName typeName, GeneratedElement name)
	{
		myTypeName = typeName;
		mySelector = name;
	}

	public TypeName getTypeName()
	{
		return myTypeName;
	}

	public GeneratedElement getSelector()
	{
		return mySelector;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("$T.$L", needNewLine), myTypeName, mySelector.generate());
	}
}
