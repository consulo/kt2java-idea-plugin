package consulo.internal.mjga.idea.convert.statement;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;

/**
 * @author VISTALL
 * @since 2020-07-09
 */
public class ThrowStatement extends Statement
{
	private final GeneratedElement myElement;

	public ThrowStatement(GeneratedElement element)
	{
		myElement = element;
	}

	@Override
	protected boolean isAllowSemicolon()
	{
		return true;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("throw $L", needNewLine), myElement.generate());
	}
}
