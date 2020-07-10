package consulo.internal.mjga.idea.convert.statement;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;

/**
 * @author VISTALL
 * @since 2020-07-10
 */
public class WhileStatement extends Statement
{
	private final GeneratedElement myCondition;
	private final GeneratedElement myBlock;

	public WhileStatement(GeneratedElement condition, GeneratedElement block)
	{
		myCondition = condition;
		myBlock = block;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.beginControlFlow("while($L)", myCondition.generate());
		builder.add(myBlock.generate());
		builder.endControlFlow();
		return builder.build();
	}
}
