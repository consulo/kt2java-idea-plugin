package consulo.internal.mjga.idea.convert.expression;

import consulo.internal.mjga.idea.convert.GeneratedElement;

import java.util.List;

/**
 * @author VISTALL
 * @since 18/02/2021
 */
public class ArrayAccessExpression extends CallExpression
{
	public ArrayAccessExpression(GeneratedElement call, List<GeneratedElement> arguments)
	{
		super(call, arguments);
	}

	@Override
	public String getTemplate()
	{
		return "$L[$L]";
	}
}
