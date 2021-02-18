package consulo.internal.mjga.idea.convert.statement;

import com.intellij.psi.PsiElement;
import consulo.internal.mjga.idea.convert.GeneratedElement;
import consulo.internal.mjga.idea.convert.expression.BinaryExpression;
import consulo.internal.mjga.idea.convert.expression.ReferenceExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtReturnExpression;

/**
 * @author VISTALL
 * @since 18/02/2021
 */
public class ExpressionToStatementMapper
{
	private static final ExpressionToStatementMapper AS_IS = new ExpressionToStatementMapper();

	private static final ExpressionToStatementMapper RETURN = new ExpressionToStatementMapper()
	{
		@Override
		public @NotNull GeneratedElement map(GeneratedElement element)
		{
			return new ReturnStatement(element);
		}
	};

	private static class Var extends ExpressionToStatementMapper
	{
		private final String myName;

		public Var(String name)
		{
			myName = name;
		}

		@Override
		public @NotNull GeneratedElement map(GeneratedElement element)
		{
			return new ExpressionStatement(new BinaryExpression(new ReferenceExpression(myName), element, "="));
		}
	}

	@NotNull
	public GeneratedElement map(GeneratedElement element)
	{
		return element;
	}

	public static ExpressionToStatementMapper find(@NotNull KtExpression ktExpression)
	{
		PsiElement parent = ktExpression.getParent();

		if(parent instanceof KtReturnExpression)
		{
			return RETURN;
		}
		else if(parent instanceof KtProperty)
		{
			return new Var(((KtProperty) parent).getName());
		}

		return AS_IS;
	}
}
