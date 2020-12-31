package consulo.internal.mjga.idea.convert.statement;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 31/12/2020
 */
public class BreakStatement extends Statement
{
	private GeneratedElement myExpression;

	public BreakStatement(@Nullable GeneratedElement expression)
	{
		myExpression = expression;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		if(myExpression != null)
		{
			return CodeBlock.of(wrap("break $L", needNewLine), myExpression.generate());
		}
		return CodeBlock.of(wrap("break", needNewLine));
	}

	@Override
	protected boolean isAllowSemicolon()
	{
		return true;
	}
}