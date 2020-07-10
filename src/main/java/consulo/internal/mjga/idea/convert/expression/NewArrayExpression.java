package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.GeneratedElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2020-07-10
 */
public class NewArrayExpression extends Expression
{
	private final TypeName myTypeName;
	private final List<GeneratedElement> myInner;

	public NewArrayExpression(TypeName typeName, List<GeneratedElement> inner)
	{
		myTypeName = typeName;
		myInner = inner;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("new $T {$L}", needNewLine), myTypeName, CodeBlock.join(myInner.stream().map(GeneratedElement::generate).collect(Collectors.toList()), ", "));
	}
}
