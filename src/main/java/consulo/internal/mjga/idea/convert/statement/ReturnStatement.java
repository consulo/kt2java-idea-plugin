package consulo.internal.mjga.idea.convert.statement;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2020-07-07
 */
public class ReturnStatement extends Statement
{
	private GeneratedElement myExpression;

	public ReturnStatement(@Nullable GeneratedElement expression)
	{
		myExpression = expression;
	}

	@Override
	public CodeBlock generate()
	{
		if(myExpression != null)
		{
			return CodeBlock.of("return $L;\n", myExpression.generate());
		}
		return CodeBlock.of("return;\n");
	}
}
