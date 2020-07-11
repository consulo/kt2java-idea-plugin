package consulo.internal.mjga.idea.convert.statement;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.expression.Expression;

/**
 * @author VISTALL
 * @since 2020-07-11
 */
public class ExpressionStatement extends Statement
{
	private final Expression myExpression;

	public ExpressionStatement(Expression expression)
	{
		myExpression = expression;
	}

	@Override
	protected boolean isAllowSemicolon()
	{
		return true;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("$L", needNewLine), myExpression.generate());
	}
}
