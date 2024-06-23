package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.GeneratedElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2020-07-09
 */
public class NewExpression extends Expression
{
	protected final TypeName myTypeName;
	protected final List<GeneratedElement> myArguments;
	protected final List<TypeName> myTypeArguments;

	public NewExpression(TypeName typeName, List<GeneratedElement> arguments)
	{
		this(typeName, List.of(), arguments);
	}

	public NewExpression(TypeName typeName, List<TypeName> typeArguments, List<GeneratedElement> arguments)
	{
		myTypeName = typeName;
		myTypeArguments = typeArguments;
		myArguments = arguments;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		CodeBlock args = CodeBlock.join(myArguments.stream().map(GeneratedElement::generate).collect(Collectors.toList()), ", ");
		if (myTypeName instanceof ArrayTypeName)
		{
			return CodeBlock.of(wrap("new $T[$L]", needNewLine), ((ArrayTypeName) myTypeName).componentType, args);
		}

		if (myTypeArguments.isEmpty())
		{
			return CodeBlock.of(wrap("new $T($L)", needNewLine), myTypeName, args);
		}
		else
		{
			String typeArgParts = myTypeArguments.stream().map(typeName -> "$T").collect(Collectors.joining(", "));

			List<Object> generationArguments = new ArrayList<>();
			generationArguments.add(myTypeName);
			generationArguments.addAll(myTypeArguments);
			generationArguments.add(args);

			return CodeBlock.of(wrap("new $T<" + typeArgParts + ">($L)", needNewLine), generationArguments.toArray());
		}
	}
}
