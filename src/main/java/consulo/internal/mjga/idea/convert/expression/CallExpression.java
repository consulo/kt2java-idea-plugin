package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 18/02/2021
 */
public abstract class CallExpression extends Expression
{
	protected GeneratedElement myCall;

	protected List<GeneratedElement> myArguments;

	public CallExpression(GeneratedElement call, List<GeneratedElement> arguments)
	{
		myCall = call;
		myArguments = arguments;
	}

	public GeneratedElement getCall()
	{
		return myCall;
	}

	public List<GeneratedElement> getArguments()
	{
		return myArguments;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap(getTemplate(), needNewLine), myCall.generate(), CodeBlock.join(myArguments.stream().map(GeneratedElement::generate).collect(Collectors.toList()), ", "));
	}

	public abstract String getTemplate();
}