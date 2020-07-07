package consulo.internal.mjga.idea.convert;

import com.intellij.psi.PsiElement;
import com.squareup.javapoet.CodeBlock;
import consulo.internal.mjga.idea.convert.expression.ConstantExpression;
import consulo.internal.mjga.idea.convert.statement.BlockStatement;
import consulo.internal.mjga.idea.convert.statement.ReturnStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2020-06-24
 */
public class KtExpressionConveter extends KtVisitorVoid
{
	public static GeneratedElement convert(PsiElement element)
	{
		KtExpressionConveter conveter = new KtExpressionConveter();
		element.accept(conveter);
		return conveter.myGeneratedElement;
	}

	private GeneratedElement myGeneratedElement;

	@Override
	public void visitElement(@NotNull PsiElement element)
	{
		super.visitElement(element);
	}

	@Override
	public void visitConstantExpression(@NotNull KtConstantExpression expression)
	{
		myGeneratedElement = new ConstantExpression(expression.getText());
	}

	@Override
	public void visitReturnExpression(KtReturnExpression expression)
	{
		KtExpression returnedExpression = expression.getReturnedExpression();
		myGeneratedElement = returnedExpression == null ? new ReturnStatement(null) : new ReturnStatement(convert(returnedExpression));
	}

	@Override
	public void visitBlockExpression(KtBlockExpression expression)
	{
		List<KtExpression> statements = expression.getStatements();

		List<GeneratedElement> generatedElements = new ArrayList<>();
		for(KtExpression statement : statements)
		{
			GeneratedElement generatedElement = convert(statement);

			if(generatedElement != null)
			{
				generatedElements.add(generatedElement);
			}
		}

		myGeneratedElement = new BlockStatement(generatedElements);
	}

	public CodeBlock getCodeBlock()
	{
		if(myGeneratedElement != null)
		{
			return myGeneratedElement.generate();
		}
		return null;
	}
}
