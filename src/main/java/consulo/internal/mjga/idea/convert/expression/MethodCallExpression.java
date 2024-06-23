package consulo.internal.mjga.idea.convert.expression;

import com.squareup.javapoet.TypeName;
import consulo.internal.mjga.idea.convert.GeneratedElement;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2020-07-08
 */
public class MethodCallExpression extends CallExpression
{
	public MethodCallExpression(GeneratedElement call, List<GeneratedElement> arguments)
	{
		super(call, arguments);
	}

	@Override
	public String getTemplate()
	{
		return "$L($L)";
	}

	@Override
	public Expression modifyToByExtensionCall(GeneratedElement receiverGenerate, TypeName qualifiedType)
	{
		GeneratedElement oldCall = this.getCall();
		List<GeneratedElement> oldArguments = this.getArguments();

		ArrayList<GeneratedElement> newArgs = new ArrayList<>(oldArguments);
		newArgs.add(0, receiverGenerate);

		MethodCallExpression newCall = new MethodCallExpression(oldCall, newArgs);
		return qualifiedType == null ? newCall : new StaticTypeQualifiedExpression(qualifiedType, newCall);
	}
}
