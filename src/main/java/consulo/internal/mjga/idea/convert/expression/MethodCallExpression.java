package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class MethodCallExpression extends Expression
{
	private GeneratedElement myCall;

	private List<GeneratedElement> myArguments;

	public MethodCallExpression(GeneratedElement call, List<GeneratedElement> arguments)
	{
		myCall = call;
		myArguments = arguments;
	}

	@Override
	public CodeBlock generate()
	{
		return CodeBlock.of("$L($L)", myCall.generate(), CodeBlock.join(myArguments.stream().map(GeneratedElement::generate).collect(Collectors.toList()), ", "));
	}
}
