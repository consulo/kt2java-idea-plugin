package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.GeneratedElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2020-07-09
 */
public class NewExpression extends Expression
{
	private TypeName myTypeName;
	private List<GeneratedElement> myArguments;

	public NewExpression(TypeName typeName, List<GeneratedElement> arguments)
	{
		myTypeName = typeName;
		myArguments = arguments;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("new $T($L)", needNewLine), myTypeName, CodeBlock.join(myArguments.stream().map(GeneratedElement::generate).collect(Collectors.toList()), ", "));
	}
}
