package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class StringBuilderExpression extends Expression
{
	private List<GeneratedElement> myList;

	public StringBuilderExpression(List<GeneratedElement> list)
	{
		myList = list;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.join(myList.stream().map(GeneratedElement::generate).collect(Collectors.toList()), " + ");
	}
}
