package consulo.internal.mjga.idea.convert.statement;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2020-07-07
 */
public class IfStatement extends Statement
{
	private GeneratedElement myCondition;

	private GeneratedElement myTrueBlock;

	private GeneratedElement myFalseBlock;

	public IfStatement(@Nonnull GeneratedElement condition, @Nonnull GeneratedElement trueBlock, @Nullable GeneratedElement falseBlock)
	{
		myCondition = condition;
		myTrueBlock = trueBlock;
		myFalseBlock = falseBlock;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		CodeBlock.Builder builder = CodeBlock.builder();

		builder.beginControlFlow("if ($L)", myCondition.generate());

		builder.add(myTrueBlock.generate());

		builder.endControlFlow();

		if(myFalseBlock != null)
		{
			builder.beginControlFlow("else");
			builder.add(myFalseBlock.generate());
			builder.endControlFlow();
		}

		return builder.build();
	}
}
