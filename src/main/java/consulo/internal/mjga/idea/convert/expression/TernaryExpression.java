package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.GeneratedElement;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class TernaryExpression extends Expression
{
	private GeneratedElement myCondition;
	private GeneratedElement myTrue;
	private GeneratedElement myFalse;

	public TernaryExpression(GeneratedElement condition, GeneratedElement aTrue, GeneratedElement aFalse)
	{
		myCondition = condition;
		myTrue = aTrue;
		myFalse = aFalse;
	}

	@Override
	public CodeBlock generate(boolean needNewLine)
	{
		return CodeBlock.of(wrap("$L ? $L : $L", needNewLine), myCondition.generate(), myTrue.generate(), myFalse.generate());
	}
}
