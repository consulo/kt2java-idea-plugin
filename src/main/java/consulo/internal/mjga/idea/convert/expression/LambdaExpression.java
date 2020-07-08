package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;

import java.util.List;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class LambdaExpression extends Expression
{
	private List<CodeBlock> myParameters;

	private GeneratedElement myBlock;

	public LambdaExpression(List<CodeBlock> parameters, GeneratedElement block)
	{
		myParameters = parameters;
		myBlock = block;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		CodeBlock.Builder builder = CodeBlock.builder();

		builder.beginControlFlow("($L) ->", CodeBlock.join(myParameters, ", "));
		builder.addStatement(myBlock.generate());
		builder.endControlFlow();
		return builder.build();
	}
}
