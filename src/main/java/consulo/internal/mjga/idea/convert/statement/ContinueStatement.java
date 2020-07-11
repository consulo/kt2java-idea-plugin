package consulo.internal.mjga.idea.convert.statement;

import com.squareup.javapoet.CodeBlock;

/**
 * @author VISTALL
 * @since 2020-07-11
 */
public class ContinueStatement extends Statement
{
	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("continue", needNewLine));
	}

	@Override
	protected boolean isAllowSemicolon()
	{
		return true;
	}
}
